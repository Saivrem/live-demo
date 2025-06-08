INSERT INTO languages (lang_id, int_lang_name, short_code, updated)
VALUES (1, 'English', 'en', 1609459200000),
       (2, 'Spanish', 'es', 1609459200000);

INSERT INTO language_names (lang_name_id, translation_lang_id, target_lang_id, name_translation, updated)
VALUES (1, 1, 1, 'English', 1609459200000),
       (2, 1, 2, 'Spanish', 1609459200000),
       (3, 2, 1, 'Inglés', 1609459200000),
       (4, 2, 2, 'Español', 1609459200000);