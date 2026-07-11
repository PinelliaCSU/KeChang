-- =============================================================
-- 课畅 - 编程题种子数据
-- 包含 C、C++、Python 三种语言的示例编程题
-- =============================================================

-- 示例1: C语言 - 两数之和（简单）
INSERT INTO question (question_type, content, answer, analysis, score, difficulty, knowledge_point, course_id, create_by, programming_config)
VALUES (
    'CODE',
    '编写一个程序，输入两个整数 a 和 b，输出它们的和。\n\n输入格式：\n一行两个整数，用空格分隔。\n\n输出格式：\n一个整数，表示两数之和。',
    '',
    '本题考察基本的输入输出和加法运算。注意使用 scanf/printf 进行格式化输入输出。',
    10, 1, 'C语言基础-输入输出', NULL, NULL,
    JSON_OBJECT(
        'language', 'c',
        'templateCode', '#include <stdio.h>\n\nint main() {\n    int a, b;\n    scanf("%d %d", &a, &b);\n    printf("%d\\n", a + b);\n    return 0;\n}',
        'timeLimit', 3,
        'memoryLimit', 128,
        'scoringMode', 'perCase',
        'testCases', JSON_ARRAY(
            JSON_OBJECT('input', '1 2', 'expectedOutput', '3'),
            JSON_OBJECT('input', '10 20', 'expectedOutput', '30'),
            JSON_OBJECT('input', '-5 8', 'expectedOutput', '3')
        ),
        'hiddenTestCases', JSON_ARRAY(
            JSON_OBJECT('input', '100 200', 'expectedOutput', '300'),
            JSON_OBJECT('input', '0 0', 'expectedOutput', '0')
        )
    )
);

-- 示例2: C++ - 判断素数（中等）
INSERT INTO question (question_type, content, answer, analysis, score, difficulty, knowledge_point, course_id, create_by, programming_config)
VALUES (
    'CODE',
    '编写一个程序，判断输入的正整数 n 是否为素数（质数）。如果是素数输出 "YES"，否则输出 "NO"。\n\n输入格式：\n一行一个正整数 n (1 <= n <= 10^6)。\n\n输出格式：\n"YES" 或 "NO"。',
    '',
    '判断素数需要检查 2 到 sqrt(n) 之间的所有整数。可使用 i*i <= n 作为循环条件避免浮点运算。注意 1 不是素数。',
    15, 3, 'C++基础-循环与判断', NULL, NULL,
    JSON_OBJECT(
        'language', 'cpp',
        'templateCode', '#include <iostream>\n#include <cmath>\nusing namespace std;\n\nint main() {\n    int n;\n    cin >> n;\n    // 在此编写代码\n    return 0;\n}',
        'timeLimit', 5,
        'memoryLimit', 256,
        'scoringMode', 'perCase',
        'testCases', JSON_ARRAY(
            JSON_OBJECT('input', '7', 'expectedOutput', 'YES'),
            JSON_OBJECT('input', '10', 'expectedOutput', 'NO'),
            JSON_OBJECT('input', '2', 'expectedOutput', 'YES')
        ),
        'hiddenTestCases', JSON_ARRAY(
            JSON_OBJECT('input', '1', 'expectedOutput', 'NO'),
            JSON_OBJECT('input', '999983', 'expectedOutput', 'YES'),
            JSON_OBJECT('input', '1000000', 'expectedOutput', 'NO')
        )
    )
);

-- 示例3: Python - 斐波那契数列（中等）
INSERT INTO question (question_type, content, answer, analysis, score, difficulty, knowledge_point, course_id, create_by, programming_config)
VALUES (
    'CODE',
    '编写一个程序，输入一个整数 n，输出斐波那契数列的第 n 项（从 0 开始，F(0)=0, F(1)=1）。\n\n输入格式：\n一行一个整数 n (0 <= n <= 30)。\n\n输出格式：\n一个整数，表示 F(n) 的值。',
    '',
    '可使用迭代法（循环）或递归法。考虑到 n <= 30，两种方法均可。推荐迭代法，时间复杂度 O(n)，空间复杂度 O(1)。',
    10, 2, 'Python基础-循环', NULL, NULL,
    JSON_OBJECT(
        'language', 'python',
        'templateCode', 'def fibonacci(n):\n    # 在此编写代码\n    pass\n\nn = int(input())\nprint(fibonacci(n))',
        'timeLimit', 3,
        'memoryLimit', 128,
        'scoringMode', 'perCase',
        'testCases', JSON_ARRAY(
            JSON_OBJECT('input', '0', 'expectedOutput', '0'),
            JSON_OBJECT('input', '5', 'expectedOutput', '5'),
            JSON_OBJECT('input', '10', 'expectedOutput', '55')
        ),
        'hiddenTestCases', JSON_ARRAY(
            JSON_OBJECT('input', '20', 'expectedOutput', '6765'),
            JSON_OBJECT('input', '30', 'expectedOutput', '832040')
        )
    )
);

-- 示例4: C++ - 冒泡排序（困难）
INSERT INTO question (question_type, content, answer, analysis, score, difficulty, knowledge_point, course_id, create_by, programming_config)
VALUES (
    'CODE',
    '编写一个冒泡排序程序。输入首先是一个整数 n（1 <= n <= 100），然后是 n 个待排序的整数。请将它们按从小到大的顺序输出，每个数之间用空格分隔。\n\n输入格式：\n第一行：整数 n\n第二行：n 个整数，空格分隔\n\n输出格式：\n一行，排序后的 n 个整数，空格分隔',
    '',
    '冒泡排序的基本思想：重复遍历数组，比较相邻元素，如果顺序错误就交换。遍历 n-1 轮即可完成排序。时间复杂度 O(n²)。',
    20, 4, 'C++-排序算法', NULL, NULL,
    JSON_OBJECT(
        'language', 'cpp',
        'templateCode', '#include <iostream>\n#include <vector>\nusing namespace std;\n\nint main() {\n    int n;\n    cin >> n;\n    vector<int> arr(n);\n    for (int i = 0; i < n; i++) cin >> arr[i];\n    // 在此实现冒泡排序\n    return 0;\n}',
        'timeLimit', 5,
        'memoryLimit', 256,
        'scoringMode', 'perCase',
        'testCases', JSON_ARRAY(
            JSON_OBJECT('input', '5\n3 1 4 1 5', 'expectedOutput', '1 1 3 4 5'),
            JSON_OBJECT('input', '3\n10 5 8', 'expectedOutput', '5 8 10')
        ),
        'hiddenTestCases', JSON_ARRAY(
            JSON_OBJECT('input', '6\n1 2 3 4 5 6', 'expectedOutput', '1 2 3 4 5 6'),
            JSON_OBJECT('input', '10\n10 9 8 7 6 5 4 3 2 1', 'expectedOutput', '1 2 3 4 5 6 7 8 9 10')
        )
    )
);
