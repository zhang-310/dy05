package cn.gaifan.douyinOperations.module.agent.skill;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能注册中心：按 name 注册与查找
 */
@Component
public class SkillRegistry {

    private final Map<String, Skill> skills = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<Skill> skillBeans;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (skillBeans != null) {
            for (Skill s : skillBeans) {
                register(s);
            }
        }
    }

    public void register(Skill skill) {
        if (skill != null && skill.getName() != null) {
            skills.put(skill.getName().toLowerCase(), skill);
        }
    }

    public Optional<Skill> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return Optional.ofNullable(skills.get(name.toLowerCase()));
    }

    /** 根据用户输入匹配首个命中的技能 */
    public Optional<Skill> findSkillForInput(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        return skills.values().stream().filter(s -> s.matches(input)).findFirst();
    }

    public List<Skill> listAll() {
        return List.copyOf(skills.values());
    }

    public List<Skill> getAll() {
        return listAll();
    }
}
