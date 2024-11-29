-- Insert Parents
INSERT INTO parent ("name", surname, "tgId")
VALUES ('Laura', 'Taylor', 40001),
       ('Michael', 'Taylor', 40002),
       ('Sarah', 'Anderson', 40003);

-- Insert Students
INSERT INTO student ("name", surname, "tgId")
VALUES ('Alice', 'Taylor', 10001),
       ('Bob', 'Taylor', 10002),
       ('Charlie', 'Taylor', 10003),
       ('David', 'Anderson', 10004);

-- Insert Teachers
INSERT INTO teacher ("name", surname, "tgId")
VALUES ('Dr. Alan', 'Turing', 20001),
       ('Dr. Marie', 'Curie', 20002),
       ('Dr. Richard', 'Feynman', 20003),
       ('Dr. Ada', 'Lovelace', 20004);

-- Insert Admins
INSERT INTO admin ("tgId")
VALUES (30001),
       (30002),
       (30003),
       (30004),
       (30005);

-- Insert Courses
INSERT INTO course (description)
VALUES ('Linear Algebra'),
       ('Calculus I'),
       ('Calculus II'),
       ('Data Structures and Algorithms');

-- Insert Assignments (linked to Courses)
INSERT INTO assignment (description, "courseId")
VALUES ('Matrices', 1),

       ('Derivatives', 2),
       ('Integrals', 2);

-- Insert Problems (linked to Assignments)
INSERT INTO problem ("number", description, "maxScore", "assignmentId")
VALUES
    -- Linear Algebra
    ('1.1', 'Compute the inverse of a 3x3 matrix.', 10, 1),
    ('1.2', 'Perform matrix multiplication of two 3x3 matrices.', 10, 1),
    ('1.3', 'Find the rank of a given matrix.', 10, 1),
    ('1.4', 'Diagonalize a 2x2 matrix.', 10, 1),

    -- Derivatives
    ('1.1', 'Find the derivative of x^3 + 5x^2 - 3x + 7.', 10, 2),
    ('1.2', 'Calculate the derivative of sin(x) * cos(x).', 10, 2),
    ('1.3', 'Determine the second derivative of e^(2x).', 10, 2),
    ('1.4', 'Find the derivative of ln(x^2 + 1).', 10, 2),
    ('1.5', 'Apply the product rule to differentiate x^2 * e^x.', 10, 2),
    ('1.6', 'Differentiate the function using the chain rule: (3x^2 + 2)^4.', 10, 2),

    -- Integrals
    ('2.1', 'Evaluate the integral of 2x dx.', 10, 3),
    ('2.2', 'Find the integral of cos(x) dx.', 10, 3),
    ('2.3', 'Calculate the definite integral of x^2 from 0 to 3.', 10, 3),
    ('2.4', 'Integrate e^(3x) dx.', 10, 3),
    ('2.5', 'Solve the integral of 1 / (x^2 + 1) dx.', 10, 3);


-- Insert Solutions (linked to Problems and Students)
INSERT INTO solution ("studentId", "chatId", "messageId", "problemId", "content", "timestamp")
VALUES (1, 5001, 6001, 5, 'The derivative of \( x^3 + 5x^2 - 3x + 7 \) is \( 3x^2 + 10x - 3 \).',
        CURRENT_TIMESTAMP - interval '3 day'),
       (1, 5001, 6002, 7,
        'The first derivative of \( e^{2x} \) is \( 2e^{2x} \), and differentiating again gives the second derivative \( 4e^{2x} \).',
        CURRENT_TIMESTAMP - interval '3 day'),
       (1, 5001, 6003, 9, '\( (x^2 e^x)'' = (2x)e^x + (x^2)e^x = e^x(2x + x^2). \)',
        CURRENT_TIMESTAMP - interval '3 day'),
       (2, 5002, 6003, 5, 'The derivative of \( x^3 + 5x^2 - 3x + 7 \) is \( x^2 + 5x - 3 \).',
        CURRENT_TIMESTAMP - interval '2 day'),
       (3, 5003, 6004, 5, 'The derivative of \( x^3 + 5x^2 - 3x + 7 \) is \( 3x^2 + 10x - 3 + 7\).',
        CURRENT_TIMESTAMP - interval '1 day');

-- Insert Assessments (linked to Solutions and Teachers)
INSERT INTO assessment ("solutionId", "teacherId", grade, "comment", "timestamp")
VALUES (1, 3, 10, 'Perfect!', CURRENT_TIMESTAMP - interval '30 minute'),
       (2, 3, 10, 'Good', CURRENT_TIMESTAMP),
       (3, 3, 4,
        'The derivative of \( x^3 \) is \( 3x^2 \) and not just \( x^2 \). Other derivatives are also incorrect. Fix it.',
        CURRENT_TIMESTAMP - interval '1 hour'),
       (4, 4, 1, 'Bruh. You are literally dumb', CURRENT_TIMESTAMP - interval '7 hour');

-- Insert Course-Student Relations
INSERT INTO coursestudents ("studentId", "courseId")
VALUES (1, 1),
       (2, 1),
       (3, 1),
       (4, 1),
       (1, 2),
       (2, 2),
       (3, 2);

-- Insert Course-Teacher Relations
INSERT INTO courseteachers ("teacherId", "courseId")
VALUES (1, 1),
       (2, 1),
       (3, 2),
       (4, 2),
       (3, 3),
       (4, 3);

-- Insert Parent-Student Relations
INSERT INTO parentstudents ("parentId", "studentId")
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (2, 1),
       (2, 2),
       (2, 3),
       (3, 4);
