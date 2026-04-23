-- V056: Phase 5 BGM 素材库初始化数据（P1-5）
DO $$
BEGIN
  IF (SELECT COUNT(*) FROM sv_bgm_library WHERE deleted = 0) < 10 THEN
    INSERT INTO sv_bgm_library (bgm_name, artist, style, bpm, mood, energy_level, emotion_curve_match, license_type, duration_seconds, tags, deleted) VALUES
    ('Peaceful Morning', 'Piano Dreams', 'luxury_piano', 72, '温暖', 3, 'slow_build', 'free', 180, '["种草","测评","护肤"]'::jsonb, 0),
    ('Gentle Touch', 'Soft Keys', 'luxury_piano', 80, '治愈', 4, 'slow_build', 'free', 150, '["护肤","日常","vlog"]'::jsonb, 0),
    ('Crystal Clear', 'Piano Moods', 'luxury_piano', 68, '宁静', 2, 'suspense', 'free', 200, '["精华","成分","科普"]'::jsonb, 0),
    ('Sunny Day Walk', 'Folk Vibes', 'energetic_folk', 120, '欢快', 7, 'rollercoaster', 'free', 160, '["日常","分享","开箱"]'::jsonb, 0),
    ('Morning Coffee', 'Acoustic Life', 'energetic_folk', 110, '轻松', 6, 'hook_climax', 'free', 140, '["日常","好物","推荐"]'::jsonb, 0),
    ('Heartstrings', 'String Ensemble', 'emotional_strings', 90, '感动', 5, 'emotional_wave', 'free', 190, '["故事","种草","情感"]'::jsonb, 0),
    ('Tears of Joy', 'Orchestra Light', 'emotional_strings', 85, '温暖', 4, 'slow_build', 'free', 210, '["感人","分享","真实"]'::jsonb, 0),
    ('Energy Boost', 'Pop Factory', 'upbeat_pop', 128, '激昂', 8, 'hook_climax', 'free', 120, '["促销","秒杀","限时"]'::jsonb, 0),
    ('Dance Floor', 'Beat Makers', 'upbeat_pop', 135, '兴奋', 9, 'rollercoaster', 'free', 130, '["活动","福利","抽奖"]'::jsonb, 0),
    ('Study Session', 'Lofi Beats', 'chill_lofi', 85, '放松', 3, 'slow_build', 'free', 240, '["教程","科普","成分"]'::jsonb, 0),
    ('Rainy Afternoon', 'Chill Hop', 'chill_lofi', 78, '慵懒', 2, 'suspense', 'free', 200, '["护肤步骤","教程","日常"]'::jsonb, 0);
  END IF;
END $$;
