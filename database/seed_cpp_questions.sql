-- ============================================================
-- C++ 题库种子数据（约 20 题）
-- 适用课程 ID=1，创建者 ID=2（可根据实际情况修改）
-- 执行前请确认 question 表中不存在重复题目
-- ============================================================

SET @course_cpp = 'C++程序设计';
SET @course_id  = 1;
SET @teacher_id = 2;

START TRANSACTION;

INSERT INTO question
(question_type, content, options, answer, analysis, score, difficulty, knowledge_point, course_id, create_by, create_time, update_time, is_deleted)
SELECT q.question_type, q.content, q.options, q.answer, q.analysis,
       q.score, q.difficulty, q.knowledge_point, @course_id, @teacher_id, NOW(), NOW(), 0
FROM (
    -- ==================== 单选题（8 题） ====================
    SELECT 'SINGLE' AS question_type,
           'C++ 中，以下哪个关键字用于声明虚函数？' AS content,
           '[{"label":"A","content":"virtual"},{"label":"B","content":"override"},{"label":"C","content":"abstract"},{"label":"D","content":"static"}]' AS options,
           'A' AS answer,
           'virtual 是声明虚函数的关键字；override 用于显式标明重写；C++ 没有 abstract 关键字（纯虚函数用 =0 表示）；static 是静态成员关键字。' AS analysis,
           5.00 AS score, 1 AS difficulty, @course_cpp AS knowledge_point

    UNION ALL SELECT 'SINGLE',
           '以下关于引用的说法，正确的是？' AS content,
           '[{"label":"A","content":"引用必须在定义时初始化"},{"label":"B","content":"引用可以重新绑定到另一个变量"},{"label":"C","content":"可以定义空引用"},{"label":"D","content":"引用和指针完全相同"}]' AS options,
           'A' AS answer,
           '引用定义时必须初始化且不能重新绑定；不存在空引用；引用是别名，指针是变量，两者不同。' AS analysis,
           5.00, 1, @course_cpp

    UNION ALL SELECT 'SINGLE',
           '以下哪个 STL 容器底层由红黑树实现？' AS content,
           '[{"label":"A","content":"vector"},{"label":"B","content":"deque"},{"label":"C","content":"map"},{"label":"D","content":"unordered_map"}]' AS options,
           'C' AS answer,
           'map/set 底层为红黑树（有序）；vector/deque 为动态数组；unordered_map 为哈希表。' AS analysis,
           5.00, 2, @course_cpp

    UNION ALL SELECT 'SINGLE',
           'C++11 引入的 `auto` 关键字的作用是？' AS content,
           '[{"label":"A","content":"声明静态变量"},{"label":"B","content":"自动推导变量类型"},{"label":"C","content":"定义自动存储期变量"},{"label":"D","content":"声明内联函数"}]' AS options,
           'B' AS answer,
           'C++11 中 auto 用于编译期自动类型推导；static 声明静态变量；auto 在 C 语言中的旧含义（自动存储期）已被弃用。' AS analysis,
           5.00, 2, @course_cpp

    UNION ALL SELECT 'SINGLE',
           '以下哪个运算符不能被重载？' AS content,
           '[{"label":"A","content":"+"},{"label":"B","content":"->"},{"label":"C","content":"::"},{"label":"D","content":"[]"}]' AS options,
           'C' AS answer,
           '::（作用域解析）、.（成员访问）、.*（成员指针访问）、?:（三目运算符）、sizeof、typeid 不能被重载。' AS analysis,
           5.00, 3, @course_cpp

    UNION ALL SELECT 'SINGLE',
           '对于 `int* p = new int[10];`，正确的释放方式是？' AS content,
           '[{"label":"A","content":"delete p;"},{"label":"B","content":"delete[] p;"},{"label":"C","content":"free(p);"},{"label":"D","content":"delete p[];"}]' AS options,
           'B' AS answer,
           'new[] 必须搭配 delete[]；delete 只释放单个对象；free 是 C 语言函数，不调用析构函数。混用会导致未定义行为。' AS analysis,
           5.00, 1, @course_cpp

    UNION ALL SELECT 'SINGLE',
           'C++ 中实现运行时多态的机制是？' AS content,
           '[{"label":"A","content":"函数重载"},{"label":"B","content":"模板"},{"label":"C","content":"虚函数和继承"},{"label":"D","content":"运算符重载"}]' AS options,
           'C' AS answer,
           '虚函数 + 基类指针/引用实现运行时多态（动态绑定）；函数重载和模板是编译时多态（静态绑定）。' AS analysis,
           5.00, 2, @course_cpp

    UNION ALL SELECT 'SINGLE',
           '`std::move` 的作用是？' AS content,
           '[{"label":"A","content":"移动对象的内存地址"},{"label":"B","content":"将左值转换为右值引用"},{"label":"C","content":"复制对象"},{"label":"D","content":"删除对象"}]' AS options,
           'B' AS answer,
           'std::move 本质是 static_cast<T&&>，将左值强制转为右值引用，从而触发移动构造/移动赋值，本身不移动任何数据。' AS analysis,
           5.00, 3, @course_cpp

    -- ==================== 多选题（4 题） ====================
    UNION ALL SELECT 'MULTI',
           '以下属于 C++ 面向对象特性的有？' AS content,
           '[{"label":"A","content":"封装"},{"label":"B","content":"继承"},{"label":"C","content":"多态"},{"label":"D","content":"垃圾回收"}]' AS options,
           'A,B,C' AS answer,
           '面向对象三大特性：封装、继承、多态。C++ 没有内置垃圾回收机制（GC），需手动管理内存或使用智能指针。' AS analysis,
           8.00, 1, @course_cpp

    UNION ALL SELECT 'MULTI',
           '以下属于 C++11 引入的智能指针是？' AS content,
           '[{"label":"A","content":"std::unique_ptr"},{"label":"B","content":"std::shared_ptr"},{"label":"C","content":"std::auto_ptr"},{"label":"D","content":"std::weak_ptr"}]' AS options,
           'A,B,D' AS answer,
           'C++11 引入 unique_ptr、shared_ptr、weak_ptr；auto_ptr 在 C++98 中存在，C++11 已废弃，C++17 已移除。' AS analysis,
           8.00, 2, @course_cpp

    UNION ALL SELECT 'MULTI',
           '以下关于 const 的说法正确的有？' AS content,
           '[{"label":"A","content":"const 修饰的变量必须在定义时初始化"},{"label":"B","content":"const 成员函数不能修改任何成员变量"},{"label":"C","content":"const 对象只能调用 const 成员函数"},{"label":"D","content":"const 变量可以用作数组大小（C++）"}]' AS options,
           'A,B,C,D' AS answer,
           '以上均正确。A：const 变量定义时必须初始化；B：const 成员函数不能修改成员（mutable 除外）；C：const 对象不能调用非 const 函数；D：C++ 中 const 整数是编译期常量，可作为数组大小。' AS analysis,
           8.00, 2, @course_cpp

    UNION ALL SELECT 'MULTI',
           '以下 STL 容器的插入操作时间复杂度为 O(1) 的有？' AS content,
           '[{"label":"A","content":"vector 尾部插入"},{"label":"B","content":"list 任意位置插入"},{"label":"C","content":"map 插入"},{"label":"D","content":"unordered_map 平均插入"}]' AS options,
           'A,B,D' AS answer,
           'vector 尾部 push_back 均摊 O(1)；list 插入只需修改指针 O(1)；unordered_map 哈希表插入平均 O(1)；map 红黑树插入 O(logn)。' AS analysis,
           8.00, 3, @course_cpp

    -- ==================== 判断题（4 题） ====================
    UNION ALL SELECT 'JUDGE',
           'C++ 中，析构函数可以是虚函数，构造函数也可以是虚函数。' AS content,
           NULL AS options,
           '0' AS answer,
           '析构函数可以是虚函数（且基类析构通常应声明为 virtual）；构造函数不能是虚函数，因为构造时虚表尚未建立。' AS analysis,
           5.00, 2, @course_cpp

    UNION ALL SELECT 'JUDGE',
           '`std::vector` 在扩容时会将原有元素复制或移动到新的内存空间。' AS content,
           NULL AS options,
           '1' AS answer,
           'vector 底层为连续内存，扩容时分配新空间，将旧元素移动/复制到新空间（优先使用移动语义），然后释放旧空间。' AS analysis,
           5.00, 1, @course_cpp

    UNION ALL SELECT 'JUDGE',
           'C++ 中 `NULL` 和 `nullptr` 完全等价，可以互换使用。' AS content,
           NULL AS options,
           '0' AS answer,
           'NULL 本质是整数 0（或 (void*)0），nullptr 是 std::nullptr_t 类型，类型安全。在重载决议中 nullptr 不会被误解析为整数，推荐使用 nullptr。' AS analysis,
           5.00, 2, @course_cpp

    UNION ALL SELECT 'JUDGE',
           '模板函数的定义必须写在头文件中，不能分离到 .cpp 文件。' AS content,
           NULL AS options,
           '1' AS answer,
           '模板在编译期实例化，编译器需要看到完整定义。若分离到 .cpp，其他翻译单元无法实例化（除非显式实例化）。通常模板定义放在头文件中。' AS analysis,
           5.00, 3, @course_cpp

    -- ==================== 填空题（2 题） ====================
    UNION ALL SELECT 'FILL',
           'C++ 中，用于防止头文件重复包含的预处理指令是 ______。' AS content,
           NULL AS options,
           '#pragma once 或 #ifndef / #define / #endif' AS answer,
           '两种方式均可：#pragma once（简洁，非标准但广泛支持）或传统的 #ifndef #define #endif 头文件保护宏。' AS analysis,
           5.00, 1, @course_cpp

    UNION ALL SELECT 'FILL',
           'C++ 中，派生类构造函数初始化列表中调用基类构造函数的写法是：`Derived(int x) : ______ { }`（基类名为 Base）。' AS content,
           NULL AS options,
           'Base(x)' AS answer,
           '派生类构造函数的初始化列表中通过 Base(x) 显式调用基类构造函数；若省略则默认调用基类无参构造函数。' AS analysis,
           5.00, 2, @course_cpp

    -- ==================== 简答题（2 题） ====================
    UNION ALL SELECT 'ESSAY',
           '简述 C++ 中指针和引用的主要区别（至少 4 点）。' AS content,
           NULL AS options,
           '1. 指针是变量，存储地址；引用是别名，不占额外空间（实现上可能是指针）。2. 引用必须初始化，指针可以为空。3. 引用不能重新绑定，指针可以修改指向。4. 有指针的指针（多级指针），无引用的引用。5. sizeof 引用得原类型大小，sizeof 指针得指针本身大小（通常 4/8 字节）。' AS analysis,
           '指针存储地址可修改可空可多级；引用是别名必须初始化不可更改。两者各有适用场景，引用更安全简洁，指针更灵活。' AS analysis,
           12.00, 2, @course_cpp

    UNION ALL SELECT 'ESSAY',
           '简述 C++ 中 `new/delete` 与 C 语言 `malloc/free` 的主要区别。' AS content,
           NULL AS options,
           '1. new/delete 是 C++ 运算符，malloc/free 是 C 库函数。2. new 会自动计算类型大小并返回正确类型指针，malloc 需手动计算字节数并强转。3. new 会调用构造函数，delete 会调用析构函数；malloc/free 不调用构造/析构。4. new 分配失败抛 std::bad_alloc 异常，malloc 返回 NULL。5. new 可以被重载，malloc 不能。' AS analysis,
           '核心区别：new/delete 是运算符，会调构造/析构函数，类型安全；malloc/free 是函数，仅分配/释放原始内存。不可混用。' AS analysis,
           12.00, 2, @course_cpp

) q;

COMMIT;

-- 验证插入结果
SELECT question_type, content, difficulty, knowledge_point
FROM question
WHERE knowledge_point = @course_cpp
ORDER BY FIELD(question_type, 'SINGLE', 'MULTI', 'JUDGE', 'FILL', 'ESSAY'), difficulty;

-- 统计
SELECT question_type AS '题型',
       COUNT(*) AS '题数',
       SUM(score) AS '总分合计'
FROM question
WHERE knowledge_point = @course_cpp
GROUP BY question_type
ORDER BY FIELD(question_type, 'SINGLE', 'MULTI', 'JUDGE', 'FILL', 'ESSAY');
