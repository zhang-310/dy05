package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;

import java.util.*;
import java.util.stream.Collectors;

/**
 * E-4：进化任务依赖（task_no 逗号分隔）解析、环检测、就绪判断。
 */
public final class EvolveTaskDagSupport {

    private EvolveTaskDagSupport() {
    }

    public static List<String> parseDependsOnTaskNos(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    public static String serializeDependsOn(List<String> nos) {
        if (nos == null || nos.isEmpty()) {
            return null;
        }
        return nos.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().collect(Collectors.joining(","));
    }

    public static List<Long> parseTopicIdList(String topicIds) {
        if (topicIds == null || topicIds.isBlank()) {
            return List.of();
        }
        String s = topicIds.trim();
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.isEmpty()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (String part : s.split(",")) {
            String p = part.strip();
            if (p.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.parseLong(p));
            } catch (NumberFormatException ignored) {
                // skip malformed
            }
        }
        return out;
    }

    /**
     * 构建「前置完成 → 可启动后继」边：dep --&gt; child（child 依赖 dep）。
     */
    public static Map<String, Set<String>> buildAdjacency(Collection<AiEvolveTask> tasksWithDeps, String newChildTaskNo, List<String> newChildDeps) {
        Map<String, Set<String>> adj = new HashMap<>();
        for (AiEvolveTask t : tasksWithDeps) {
            String child = t.getTaskNo();
            if (child == null || child.isBlank()) {
                continue;
            }
            for (String dep : parseDependsOnTaskNos(t.getDependsOnTaskNos())) {
                adj.computeIfAbsent(dep, k -> new LinkedHashSet<>()).add(child);
            }
        }
        if (newChildTaskNo != null && !newChildTaskNo.isBlank() && newChildDeps != null) {
            for (String dep : newChildDeps) {
                adj.computeIfAbsent(dep, k -> new LinkedHashSet<>()).add(newChildTaskNo);
            }
        }
        return adj;
    }

    private static Set<String> allNodes(Map<String, Set<String>> adj) {
        Set<String> nodes = new HashSet<>(adj.keySet());
        for (Set<String> to : adj.values()) {
            nodes.addAll(to);
        }
        return nodes;
    }

    /**
     * 有向图环检测（DFS 灰集）。
     */
    public static boolean graphHasCycle(Map<String, Set<String>> adj) {
        Set<String> nodes = allNodes(adj);
        Set<String> white = new HashSet<>(nodes);
        Set<String> gray = new HashSet<>();
        Set<String> black = new HashSet<>();
        for (String n : nodes) {
            if (white.contains(n) && dfsCycleColor(adj, n, white, gray, black)) {
                return true;
            }
        }
        return false;
    }

    private static boolean dfsCycleColor(Map<String, Set<String>> adj, String u, Set<String> white, Set<String> gray, Set<String> black) {
        white.remove(u);
        gray.add(u);
        for (String v : adj.getOrDefault(u, Set.of())) {
            if (black.contains(v)) {
                continue;
            }
            if (gray.contains(v)) {
                return true;
            }
            if (white.contains(v) && dfsCycleColor(adj, v, white, gray, black)) {
                return true;
            }
        }
        gray.remove(u);
        black.add(u);
        return false;
    }

    public static boolean wouldCreateCycle(Collection<AiEvolveTask> existingWithDeps, String newTaskNo, List<String> newDeps) {
        Map<String, Set<String>> adj = buildAdjacency(existingWithDeps, newTaskNo, newDeps);
        return graphHasCycle(adj);
    }

    public static boolean allDependenciesCompleted(Map<String, String> taskNoToStatus, List<String> deps) {
        for (String d : deps) {
            String st = taskNoToStatus.get(d);
            if (!"completed".equals(st)) {
                return false;
            }
        }
        return true;
    }

    public static List<String> incompleteDependencies(Map<String, String> taskNoToStatus, List<String> deps) {
        List<String> miss = new ArrayList<>();
        for (String d : deps) {
            if (!"completed".equals(taskNoToStatus.get(d))) {
                miss.add(d);
            }
        }
        return miss;
    }

    public static String blockedReasonForIncomplete(List<String> incompleteNos) {
        return "等待前置任务完成: " + String.join(", ", incompleteNos);
    }
}
