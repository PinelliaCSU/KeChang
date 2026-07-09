-- 执行前确认：三门课都应该能查到 id
SELECT id, course_name, teacher_id, status
FROM course
WHERE is_deleted = 0
  AND course_name IN ('数据结构与算法', '操作系统', '计算机组成原理');

START TRANSACTION;

-- ============================================================
-- 数据结构与算法
-- ============================================================
INSERT INTO question
(question_type, content, options, answer, analysis, score, difficulty, knowledge_point, course_id, create_by, create_time, update_time, is_deleted)
SELECT q.question_type, q.content, q.options, q.answer, q.analysis, q.score, q.difficulty, q.knowledge_point,
       c.id, c.teacher_id, NOW(), NOW(), 0
FROM course c
         JOIN (
    SELECT 'SINGLE' AS question_type, '下列排序平均时间复杂度为O(nlogn)的是' AS content,
           '[{"label":"A","content":"冒泡排序"},{"label":"B","content":"快速排序"},{"label":"C","content":"简单选择排序"},{"label":"D","content":"直接插入排序"}]' AS options,
           'B' AS answer, '冒泡、选择、插入平均O(n²)；快排平均O(nlogn)，最坏O(n²)' AS analysis, 5.00 AS score, 2 AS difficulty, @course_ds AS knowledge_point
    UNION ALL SELECT 'SINGLE', '二叉搜索树中序遍历结果特征',
                     '[{"label":"A","content":"降序"},{"label":"B","content":"升序"},{"label":"C","content":"无序"},{"label":"D","content":"层序"}]',
                     'B', '二叉搜索树左小右大，中序左根右遍历输出升序', 5.00, 1, @course_ds
    UNION ALL SELECT 'SINGLE', '栈的逻辑结构特点',
                     '[{"label":"A","content":"先进先出"},{"label":"B","content":"先进后出"},{"label":"C","content":"随机存取"},{"label":"D","content":"两端进出"}]',
                     'B', '栈限定只能在同一端入栈出栈，后进先出LIFO', 5.00, 1, @course_ds
    UNION ALL SELECT 'MULTI', '属于稳定排序算法',
                     '[{"label":"A","content":"归并排序"},{"label":"B","content":"冒泡排序"},{"label":"C","content":"快速排序"},{"label":"D","content":"基数排序"}]',
                     'A,B,D', '快排不稳定；冒泡、归并、基数排序相等元素相对位置不变', 8.00, 2, @course_ds
    UNION ALL SELECT 'MULTI', '图的遍历方式有',
                     '[{"label":"A","content":"深度优先DFS"},{"label":"B","content":"广度优先BFS"},{"label":"C","content":"中序遍历"},{"label":"D","content":"层序遍历"}]',
                     'A,B', '中序、层序属于二叉树遍历；图标准遍历为DFS、BFS', 8.00, 1, @course_ds
    UNION ALL SELECT 'JUDGE', 'DFS适合求解无权图最短路径',
                     NULL, '0', 'BFS逐层扩散，适合无权最短路径；DFS无法保证最短', 5.00, 2, @course_ds
    UNION ALL SELECT 'JUDGE', '二分查找仅支持顺序存储有序数组',
                     NULL, '1', '二分依赖随机访问，链表无法使用二分查找', 5.00, 1, @course_ds
    UNION ALL SELECT 'ESSAY', '简述二分查找适用条件与时间复杂度',
                     NULL, '有序顺序存储、支持随机访问；O(logn)', '1.存储为顺序结构；2.序列升序/降序；每次折半缩小一半范围，对数复杂度', 12.00, 2, @course_ds
    UNION ALL SELECT 'ESSAY', '简述栈与队列的区别及典型应用',
                     NULL, '栈LIFO，队列FIFO；栈：函数调用、表达式求值；队列：任务调度、层次遍历', '栈同一端进出，队列两端分别入队出队；应用场景完全区分', 12.00, 2, @course_ds
) q
WHERE c.is_deleted = 0
  AND c.course_name = '数据结构与算法';

-- ============================================================
-- 操作系统
-- ============================================================
INSERT INTO question
(question_type, content, options, answer, analysis, score, difficulty, knowledge_point, course_id, create_by, create_time, update_time, is_deleted)
SELECT q.question_type, q.content, q.options, q.answer, q.analysis, q.score, q.difficulty, q.knowledge_point,
       c.id, c.teacher_id, NOW(), NOW(), 0
FROM course c
         JOIN (
    SELECT 'SINGLE' AS question_type, 'SJF调度算法指' AS content,
           '[{"label":"A","content":"先来先服务"},{"label":"B","content":"短作业优先"},{"label":"C","content":"时间片轮转"},{"label":"D","content":"优先级调度"}]' AS options,
           'B' AS answer, 'SJF(Shortest Job First)优先运行预估耗时最短进程' AS analysis, 5.00 AS score, 2 AS difficulty, @course_os AS knowledge_point
    UNION ALL SELECT 'SINGLE', '临界区定义是',
                     '[{"label":"A","content":"整个进程代码"},{"label":"B","content":"访问共享资源代码段"},{"label":"C","content":"进程栈"},{"label":"D","content":"PCB"}]',
                     'B', '临界区是操作互斥共享资源的代码，同一时刻仅一个进程进入', 5.00, 1, @course_os
    UNION ALL SELECT 'SINGLE', '同一进程内多线程共享',
                     '[{"label":"A","content":"栈"},{"label":"B","content":"寄存器"},{"label":"C","content":"进程地址空间"},{"label":"D","content":"局部变量"}]',
                     'C', '线程私有栈与寄存器，共享进程全局地址空间', 5.00, 1, @course_os
    UNION ALL SELECT 'MULTI', '虚拟内存页面置换算法',
                     '[{"label":"A","content":"FIFO"},{"label":"B","content":"LRU"},{"label":"C","content":"OPT最优"},{"label":"D","content":"Clock时钟"}]',
                     'A,B,C,D', '四大经典页面置换算法，OPT理论最优无法实现', 8.00, 2, @course_os
    UNION ALL SELECT 'MULTI', '死锁产生必要条件',
                     '[{"label":"A","content":"互斥"},{"label":"B","content":"占有并等待"},{"label":"C","content":"不可剥夺"},{"label":"D","content":"循环等待"}]',
                     'A,B,C,D', '四个条件同时满足才会产生死锁，破坏任意一个即可避免', 8.00, 2, @course_os
    UNION ALL SELECT 'JUDGE', '线程拥有独立地址空间',
                     NULL, '0', '线程无独立地址，全部共享所属进程虚拟地址空间', 5.00, 1, @course_os
    UNION ALL SELECT 'JUDGE', '内存分页会产生外部碎片',
                     NULL, '0', '分页固定页框，仅存在内部碎片；分段才产生外部碎片', 5.00, 2, @course_os
    UNION ALL SELECT 'ESSAY', '分页存储与分段存储核心区别',
                     NULL, '页物理固定；段逻辑可变', '分页面向硬件消除外部碎片；分段面向用户逻辑模块，存在外部碎片', 12.00, 3, @course_os
    UNION ALL SELECT 'ESSAY', '进程与线程的区别',
                     NULL, '进程资源独立；线程共享进程资源，切换开销更小', '进程是资源分配单位，线程是调度执行单位；进程切换开销远大于线程', 12.00, 2, @course_os
) q
WHERE c.is_deleted = 0
  AND c.course_name = '操作系统';

-- ============================================================
-- 计算机组成原理
-- ============================================================
INSERT INTO question
(question_type, content, options, answer, analysis, score, difficulty, knowledge_point, course_id, create_by, create_time, update_time, is_deleted)
SELECT q.question_type, q.content, q.options, q.answer, q.analysis, q.score, q.difficulty, q.knowledge_point,
       c.id, c.teacher_id, NOW(), NOW(), 0
FROM course c
         JOIN (
    SELECT 'SINGLE' AS question_type, '存放当前正在执行指令的寄存器' AS content,
           '[{"label":"A","content":"PC程序计数器"},{"label":"B","content":"IR指令寄存器"},{"label":"C","content":"ACC累加器"},{"label":"D","content":"MAR地址寄存器"}]' AS options,
           'B' AS answer, 'IR保存取出的指令；PC存下一条指令地址' AS analysis, 5.00 AS score, 1 AS difficulty, @course_co AS knowledge_point
    UNION ALL SELECT 'SINGLE', '存储层次速度从快至慢',
                     '[{"label":"A","content":"寄存器>Cache>内存>硬盘"},{"label":"B","content":"Cache>寄存器>内存>硬盘"},{"label":"C","content":"内存>Cache>寄存器>硬盘"},{"label":"D","content":"硬盘>内存>Cache>寄存器"}]',
                     'A', '距离CPU越近速度越快、成本越高', 5.00, 2, @course_co
    UNION ALL SELECT 'SINGLE', '浮点数阶码决定',
                     '[{"label":"A","content":"精度"},{"label":"B","content":"数值表示范围"},{"label":"C","content":"符号"},{"label":"D","content":"进位"}]',
                     'B', '尾数位数决定精度，阶码决定可表示数值区间', 5.00, 2, @course_co
    UNION ALL SELECT 'MULTI', '流水线冲突三大类型',
                     '[{"label":"A","content":"结构冲突"},{"label":"B","content":"数据冲突"},{"label":"C","content":"控制冲突"},{"label":"D","content":"IO冲突"}]',
                     'A,B,C', '资源、数据相关、分支指令三类冒险冲突', 8.00, 2, @course_co
    UNION ALL SELECT 'MULTI', 'Cache局部性原理包含',
                     '[{"label":"A","content":"时间局部性"},{"label":"B","content":"空间局部性"},{"label":"C","content":"逻辑局部性"},{"label":"D","content":"存储局部性"}]',
                     'A,B', '时间：刚访问很快再访问；空间：相邻地址大概率访问', 8.00, 1, @course_co
    UNION ALL SELECT 'JUDGE', '浮点数尾数越长精度越高',
                     NULL, '1', '尾数存储有效数字，位数越多小数精度越高', 5.00, 2, @course_co
    UNION ALL SELECT 'JUDGE', '机器语言可直接被CPU执行',
                     NULL, '1', '硬件仅识别二进制机器码，汇编/高级语言需翻译', 5.00, 1, @course_co
    UNION ALL SELECT 'ESSAY', '简述Cache局部性原理及作用',
                     NULL, '时间、空间局部性；减少访存延迟', '利用局部性把常用数据放高速Cache，降低访问内存耗时提升CPU吞吐', 12.00, 2, @course_co
    UNION ALL SELECT 'ESSAY', '简述指令寄存器IR与程序计数器PC作用',
                     NULL, 'IR存当前指令，PC存下一条指令地址', '取指周期：PC送地址到内存，取出指令存入IR供译码执行', 12.00, 1, @course_co
) q
WHERE c.is_deleted = 0
  AND c.course_name = '计算机组成原理';

COMMIT;
