-- Локальные демонстрационные данные. Flyway выполняет миграцию один раз.
-- Пароль всех demo-аккаунтов: SmartEduDemo2026!
-- ID выделяются последовательностями; существующие пользователи не изменяются.
-- Совпадение demo-email прервёт транзакцию вместо изменения чужого аккаунта.
DO $$
DECLARE
    admin_id BIGINT;
    teacher_a BIGINT;
    teacher_b BIGINT;
    curator_a BIGINT;
    curator_b BIGINT;
    student_a BIGINT;
    student_b BIGINT;
    student_c BIGINT;
    group_a BIGINT;
    group_b BIGINT;
    course_a BIGINT;
    course_b BIGINT;
    module_a BIGINT;
    module_b BIGINT;
    lesson_a1 BIGINT;
    lesson_a2 BIGINT;
    lesson_a3 BIGINT;
    lesson_b1 BIGINT;
    lesson_b2 BIGINT;
    lesson_b3 BIGINT;
    exam_a BIGINT;
    exam_b BIGINT;
    question_a BIGINT;
    question_b BIGINT;
    enrollment_id BIGINT;
    attempt_id BIGINT;
    topic_id BIGINT;
    demo_hash TEXT := '$2b$12$9j3eL8bQPJlj6ScUIPNSKuelM703IPMIckpfw/eCJhCuvjd1bjD8G';
BEGIN
    INSERT INTO users(email,full_name,password_hash,role) VALUES
        ('admin@demo.smartedu.local','Администратор демо',demo_hash,'ADMIN') RETURNING id INTO admin_id;
    INSERT INTO users(email,full_name,password_hash,role) VALUES
        ('teacher.java@demo.smartedu.local','Анна Смирнова',demo_hash,'TEACHER') RETURNING id INTO teacher_a;
    INSERT INTO users(email,full_name,password_hash,role) VALUES
        ('teacher.sql@demo.smartedu.local','Михаил Орлов',demo_hash,'TEACHER') RETURNING id INTO teacher_b;
    INSERT INTO users(email,full_name,password_hash,role) VALUES
        ('curator.a@demo.smartedu.local','Елена Волкова',demo_hash,'METHODIST') RETURNING id INTO curator_a;
    INSERT INTO users(email,full_name,password_hash,role) VALUES
        ('curator.b@demo.smartedu.local','Сергей Соколов',demo_hash,'METHODIST') RETURNING id INTO curator_b;
    INSERT INTO users(email,full_name,password_hash,role) VALUES
        ('student.alex@demo.smartedu.local','Алексей Иванов',demo_hash,'STUDENT') RETURNING id INTO student_a;
    INSERT INTO users(email,full_name,password_hash,role) VALUES
        ('student.maria@demo.smartedu.local','Мария Петрова',demo_hash,'STUDENT') RETURNING id INTO student_b;
    INSERT INTO users(email,full_name,password_hash,role) VALUES
        ('student.ivan@demo.smartedu.local','Иван Кузнецов',demo_hash,'STUDENT') RETURNING id INTO student_c;

    INSERT INTO study_groups(name,curator_id) VALUES ('Демо · Java-01',curator_a) RETURNING id INTO group_a;
    INSERT INTO study_groups(name,curator_id) VALUES ('Демо · Java-02',curator_b) RETURNING id INTO group_b;
    INSERT INTO study_group_students(group_id,student_id) VALUES
        (group_a,student_a),(group_a,student_b),(group_b,student_c);

    INSERT INTO courses(teacher_id,title,description,category,status) VALUES
        (teacher_a,'Демо · Основы Java','Типы данных, условия и первая программа. Курс для знакомства с учебным пространством.','Программирование','PUBLISHED') RETURNING id INTO course_a;
    INSERT INTO courses(teacher_id,title,description,category,status) VALUES
        (teacher_b,'Демо · SQL и базы данных','Таблицы, ключи и запросы. Второй курс позволяет проверить границы доступа преподавателей.','Базы данных','PUBLISHED') RETURNING id INTO course_b;
    INSERT INTO courses(teacher_id,title,description,category,status) VALUES
        (teacher_a,'Демо · Коллекции Java','Черновик следующего курса.','Программирование','DRAFT');
    INSERT INTO course_modules(course_id,title,order_index) VALUES (course_a,'Первые шаги',0) RETURNING id INTO module_a;
    INSERT INTO course_modules(course_id,title,order_index) VALUES (course_b,'Реляционная модель',0) RETURNING id INTO module_b;
    INSERT INTO lessons(module_id,type,title,content,order_index) VALUES
        (module_a,'TEXT','Переменные и типы',E'Переменная хранит значение определённого типа. int — целые числа, boolean — true или false, String — строки.\nНапример: int count = 3; boolean ready = true; String name = "Алексей";\nОбъявляйте переменные с понятными именами.',0) RETURNING id INTO lesson_a1;
    INSERT INTO lessons(module_id,type,title,content,order_index) VALUES
        (module_a,'CODE','Первая программа',E'Откройте редактор кода, выберите Java и выведите Hello, SmartEdu!\nЗатем прочитайте число из stdin и выведите его квадрат. В Java публичный класс назовите Main.',1) RETURNING id INTO lesson_a2;
    INSERT INTO lessons(module_id,type,title,content,order_index) VALUES
        (module_a,'QUIZ','Проверка знаний',E'Объясните разницу между int и String. Экзамен ниже содержит демонстрационный вопрос.\nДля новых AI-вопросов преподаватель загружает PDF, DOCX или TXT в материалы урока и ждёт индексации.',2) RETURNING id INTO lesson_a3;
    INSERT INTO lessons(module_id,type,title,content,order_index) VALUES
        (module_b,'TEXT','Таблицы и ключи',E'Таблица состоит из строк и столбцов. Первичный ключ однозначно определяет строку. Внешний ключ ссылается на строку связанной таблицы и поддерживает ссылочную целостность.',0) RETURNING id INTO lesson_b1;
    INSERT INTO lessons(module_id,type,title,content,order_index) VALUES
        (module_b,'TEXT','SELECT и WHERE',E'SELECT выбирает столбцы, FROM задаёт таблицу, WHERE фильтрует строки.\nПример: SELECT full_name FROM users WHERE role = ''STUDENT'';\nORDER BY задаёт порядок результата.',1) RETURNING id INTO lesson_b2;
    INSERT INTO lessons(module_id,type,title,content,order_index) VALUES
        (module_b,'QUIZ','Связи таблиц','Объясните назначение внешнего ключа и приведите пример связи пользователя с записью на курс.',2) RETURNING id INTO lesson_b3;

    INSERT INTO enrollments(user_id,course_id,progress_pct) VALUES (student_a,course_a,67) RETURNING id INTO enrollment_id;
    INSERT INTO lesson_progress(enrollment_id,lesson_id,completed_at,score) VALUES
        (enrollment_id,lesson_a1,now()-interval '2 days',NULL),(enrollment_id,lesson_a2,now()-interval '1 day',NULL);
    INSERT INTO enrollments(user_id,course_id,progress_pct) VALUES (student_b,course_a,33) RETURNING id INTO enrollment_id;
    INSERT INTO lesson_progress(enrollment_id,lesson_id,completed_at) VALUES (enrollment_id,lesson_a1,now()-interval '1 day');
    INSERT INTO enrollments(user_id,course_id,progress_pct) VALUES (student_c,course_a,100) RETURNING id INTO enrollment_id;
    INSERT INTO lesson_progress(enrollment_id,lesson_id,completed_at) VALUES
        (enrollment_id,lesson_a1,now()-interval '3 days'),(enrollment_id,lesson_a2,now()-interval '2 days'),(enrollment_id,lesson_a3,now()-interval '1 day');
    INSERT INTO enrollments(user_id,course_id,progress_pct) VALUES (student_a,course_b,33) RETURNING id INTO enrollment_id;
    INSERT INTO lesson_progress(enrollment_id,lesson_id,completed_at) VALUES (enrollment_id,lesson_b1,now()-interval '1 day');
    INSERT INTO enrollments(user_id,course_id,progress_pct) VALUES (student_c,course_b,0);

    INSERT INTO exams(lesson_id,title,time_limit_minutes) VALUES (lesson_a3,'Типы данных Java',15) RETURNING id INTO exam_a;
    INSERT INTO exams(lesson_id,title,time_limit_minutes) VALUES (lesson_b3,'Ключи и связи',15) RETURNING id INTO exam_b;
    INSERT INTO exam_questions(exam_id,question_text,max_score,order_index) VALUES
        (exam_a,'Чем int отличается от String? Приведите по одному примеру.',10,0) RETURNING id INTO question_a;
    INSERT INTO exam_questions(exam_id,question_text,max_score,order_index) VALUES
        (exam_b,'Зачем нужен внешний ключ? Приведите пример связи таблиц.',10,0) RETURNING id INTO question_b;
    INSERT INTO exam_attempts(exam_id,user_id,started_at,finished_at,total_score,status) VALUES
        (exam_a,student_a,now()-interval '1 day 10 minutes',now()-interval '1 day',8,'FINISHED') RETURNING id INTO attempt_id;
    INSERT INTO exam_answers(attempt_id,question_id,answer_text,score,ai_feedback) VALUES
        (attempt_id,question_a,'int хранит целые числа, например 3. String хранит строки, например "Привет".',8,'Демонстрационная оценка: основные различия указаны верно.');
    INSERT INTO exam_attempts(exam_id,user_id,started_at,finished_at,total_score,status) VALUES
        (exam_a,student_b,now()-interval '1 day 12 minutes',now()-interval '1 day',4,'FINISHED') RETURNING id INTO attempt_id;
    INSERT INTO exam_answers(attempt_id,question_id,answer_text,score,ai_feedback) VALUES
        (attempt_id,question_a,'int — число, String — текст.',4,'Демонстрационная оценка: добавьте примеры объявления переменных.');
    INSERT INTO exam_attempts(exam_id,user_id,started_at,finished_at,total_score,status) VALUES
        (exam_a,student_c,now()-interval '1 day 8 minutes',now()-interval '1 day',10,'FINISHED') RETURNING id INTO attempt_id;
    INSERT INTO exam_answers(attempt_id,question_id,answer_text,score,ai_feedback) VALUES
        (attempt_id,question_a,'int — примитивный целочисленный тип: int age = 20. String — класс для строк: String name = "Иван".',10,'Демонстрационная оценка: полный ответ с примерами.');
    INSERT INTO exam_attempts(exam_id,user_id,started_at,finished_at,total_score,status) VALUES
        (exam_b,student_a,now()-interval '5 hours 10 minutes',now()-interval '5 hours',7,'FINISHED') RETURNING id INTO attempt_id;
    INSERT INTO exam_answers(attempt_id,question_id,answer_text,score,ai_feedback) VALUES
        (attempt_id,question_b,'Внешний ключ связывает таблицы, например enrollments.user_id с users.id.',7,'Демонстрационная оценка: поясните также ссылочную целостность.');
    INSERT INTO exam_attempts(exam_id,user_id,started_at,finished_at,total_score,status) VALUES
        (exam_b,student_c,now()-interval '2 hours 15 minutes',now()-interval '2 hours',NULL,'EXPIRED');

    INSERT INTO forum_topics(course_id,author_id,title) VALUES
        (course_a,student_a,'Как выбрать тип переменной?') RETURNING id INTO topic_id;
    INSERT INTO forum_posts(topic_id,author_id,content) VALUES
        (topic_id,student_a,'Какой тип использовать для количества выполненных заданий?'),
        (topic_id,teacher_a,'Подойдёт int: количество заданий — целое число. Для названия задания используйте String.');
END $$;
