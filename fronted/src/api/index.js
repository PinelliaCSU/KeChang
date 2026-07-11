import request from './request'

/** 认证相关 API */
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  me: () => request.get('/auth/me'),
  changePassword: (data) => request.put('/auth/password', data),
  updateProfile: (data) => request.put('/auth/profile', data),
  logout: () => request.post('/auth/logout')
}

/** 课程相关 API */
export const courseApi = {
  list: (params) => request.get('/courses', { params }),
  detail: (id) => request.get(`/courses/${id}`),
  create: (data) => request.post('/courses', data),
  update: (id, data) => request.put(`/courses/${id}`, data),
  delete: (id) => request.delete(`/courses/${id}`),
  addChapter: (id, data) => request.post(`/courses/${id}/chapters`, data),
  updateChapter: (id, chapterId, data) => request.put(`/courses/${id}/chapters/${chapterId}`, data),
  deleteChapter: (id, chapterId) => request.delete(`/courses/${id}/chapters/${chapterId}`),
  uploadResource: (id, formData) => request.post(`/courses/${id}/resources`, formData),
  deleteResource: (courseId, resourceId) => request.delete(`/courses/${courseId}/resources/${resourceId}`),
  getStudents: (id, params) => request.get(`/courses/${id}/students`, { params }),
  getResources: (id, params) => request.get(`/courses/${id}/resources`, { params }),
  markResourceViewed: (courseId, resourceId) => request.post(`/courses/${courseId}/resources/${resourceId}/view`),
  summarizeResource: (courseId, resourceId) => request.post(`/courses/${courseId}/resources/${resourceId}/summarize`),
  enroll: (id) => request.post(`/courses/${id}/enroll`),
  unenroll: (id) => request.delete(`/courses/${id}/enroll`),
  removeStudent: (courseId, studentId) => request.delete(`/courses/${courseId}/students/${studentId}`)
}

/** 试题/考试 API */
export const examApi = {
  // 试题
  listQuestions: (params) => request.get('/questions', { params }),
  createQuestion: (data) => request.post('/questions', data),
  updateQuestion: (id, data) => request.put(`/questions/${id}`, data),
  deleteQuestion: (id) => request.delete(`/questions/${id}`),
  // 试卷
  listPapers: (params) => request.get('/papers', { params }),
  createPaper: (data) => request.post('/papers', data),
  aiGenerate: (data) => request.post('/papers/ai-generate', data),
  getPaper: (id) => request.get(`/papers/${id}`),
  deletePaper: (id) => request.delete(`/papers/${id}`),
  // 考试
  listExams: (params) => request.get('/exams', { params }),
  createExam: (data) => request.post('/exams', data),
  updateExam: (id, data) => request.put(`/exams/${id}`, data),
  deleteExam: (id) => request.delete(`/exams/${id}`),
  getExam: (id) => request.get(`/exams/${id}`),
  startExam: (id) => request.post(`/exams/${id}/start`),
  submitExam: (id, data) => request.post(`/exams/${id}/submit`, data),
  getRecords: (id) => request.get(`/exams/${id}/records`),
  getRecordDetail: (recordId) => request.get(`/exams/records/${recordId}`),
  mark: (recordId, params) => request.put(`/exams/records/${recordId}/mark`, null, { params })
}

/** 随堂测验 API */
export const classQuizApi = {
  teacherList: (params) => request.get('/class-quizzes/teacher', { params }),
  studentActiveList: () => request.get('/class-quizzes/student/active'),
  create: (data) => request.post('/class-quizzes', data),
  detail: (id) => request.get(`/class-quizzes/${id}`),
  stats: (id) => request.get(`/class-quizzes/${id}/stats`),
  submit: (id, data) => request.post(`/class-quizzes/${id}/answer`, data),
  close: (id) => request.put(`/class-quizzes/${id}/close`)
}

/** 课堂积分 / 随机点名 API */
export const participationApi = {
  rollCall: (data) => request.post('/participation/roll-call', data),
  award: (data) => request.post('/participation/award', data),
  scoreboard: (courseId) => request.get(`/participation/courses/${courseId}/scoreboard`),
  my: () => request.get('/participation/my')
}

/** 课堂签到 API */
export const checkinApi = {
  // 教师端
  teacherList: (params) => request.get('/checkin/teacher', { params }),
  create: (data) => request.post('/checkin', data),
  detail: (id) => request.get(`/checkin/${id}`),
  stats: (id) => request.get(`/checkin/${id}/stats`),
  close: (id) => request.put(`/checkin/${id}/close`),
  // 学生端
  studentActiveList: () => request.get('/checkin/student/active'),
  checkin: (id, data) => request.post(`/checkin/${id}/checkin`, data),
  // 课程维度
  courseRecords: (courseId) => request.get(`/checkin/course/${courseId}/records`),
  courseStats: (courseId) => request.get(`/checkin/course/${courseId}/stats`)
}

/** 作业 API */
export const assignmentApi = {
  list: (params) => request.get('/assignments', { params }),
  myList: (params) => request.get('/assignments/student/my', { params }),
  create: (data) => request.post('/assignments', data),
  detail: (id) => request.get(`/assignments/${id}`),
  update: (id, data) => request.put(`/assignments/${id}`, data),
  delete: (id) => request.delete(`/assignments/${id}`),
  submit: (id, data) => request.post(`/assignments/${id}/submit`, data),
  getSubmitList: (id, params) => request.get(`/assignments/${id}/submits`, { params }),
  downloadArchive: (id) => request.get(`/assignments/${id}/submits/archive`, { responseType: 'blob' }),
  grade: (submitId, data) => request.put(`/assignments/submits/${submitId}/grade`, data)
}

/** 成绩 API */
export const gradeApi = {
  studentGrades: (studentId) => request.get(`/grades/student/${studentId}`),
  myGrades: () => request.get('/grades/my'),
  courseGrades: (courseId) => request.get(`/grades/course/${courseId}`),
  courseStats: (courseId) => request.get(`/grades/course/${courseId}/stats`),
  platformOverview: () => request.get('/grades/platform/overview')
}

/** 实训 API */
export const trainingApi = {
  // --- 项目管理 ---
  listProjects: (params) => request.get('/trainings', { params }),
  getProject: (id) => request.get(`/trainings/${id}`),
  createProject: (data) => request.post('/trainings', data),
  updateProject: (id, data) => request.put(`/trainings/${id}`, data),
  deleteProject: (id) => request.delete(`/trainings/${id}`),

  // --- 实习计划 ---
  updatePlan: (id, data) => request.put(`/trainings/${id}/plan`, data),
  getPlanAttachments: (id) => request.get(`/trainings/${id}/plan-attachments`),

  // --- 资料 ---
  listMaterials: (projectId, params) => request.get(`/trainings/${projectId}/materials`, { params }),
  uploadMaterial: (projectId, data) => request.post(`/trainings/${projectId}/materials`, data),
  deleteMaterial: (materialId) => request.delete(`/trainings/materials/${materialId}`),

  // --- 任务 ---
  createTask: (projectId, data) => request.post(`/trainings/${projectId}/tasks`, data),
  listTasks: (projectId, params) => request.get(`/trainings/${projectId}/tasks`, { params }),
  updateTask: (taskId, data) => request.put(`/trainings/tasks/${taskId}`, data),
  deleteTask: (taskId) => request.delete(`/trainings/tasks/${taskId}`),

  // --- 报告 ---
  submitReport: (taskId, data) => request.post(`/trainings/tasks/${taskId}/report`, data),
  listReports: (taskId, params) => request.get(`/trainings/tasks/${taskId}/reports`, { params }),
  gradeReport: (reportId, params) => request.put(`/trainings/reports/${reportId}/grade`, null, { params })
}

/** 问答 API */
export const qaApi = {
  listPosts: (params) => request.get('/questions/posts', { params }),
  createPost: (data) => request.post('/questions/posts', data),
  getPost: (id) => request.get(`/questions/posts/${id}`),
  deletePost: (id) => request.delete(`/questions/posts/${id}`),
  createAnswer: (id, params) => request.post(`/questions/posts/${id}/answer`, null, { params }),
  deleteAnswer: (id) => request.delete(`/questions/answers/${id}`),
  aiAnswer: (id) => request.post(`/questions/posts/${id}/answer/ai`),
  acceptAnswer: (id) => request.put(`/questions/answers/${id}/accept`),
  addComment: (id, params) => request.post(`/questions/answers/${id}/comment`, null, { params }),
  addPostComment: (id, params) => request.post(`/questions/posts/${id}/comment`, null, { params }),
  listComments: (targetType, targetId, params) => {
    const prefix = targetType === 'POST' ? 'posts' : 'answers'
    return request.get(`/questions/${prefix}/${targetId}/comments`, { params })
  },
  listAnswers: (id, params) => request.get(`/questions/posts/${id}/answers`, { params })
}

/** 仪表盘 API */
export const dashboardApi = {
  teacherStats: () => request.get('/dashboard/teacher/stats'),
  studentStats: () => request.get('/dashboard/student/stats')
}

/** 评价 API */
export const evaluationApi = {
  list: (params) => request.get('/evaluations', { params }),
  detail: (id) => request.get(`/evaluations/${id}`),
  create: (data) => request.post('/evaluations', data),
  update: (id, data) => request.put(`/evaluations/${id}`, data),
  delete: (id) => request.delete(`/evaluations/${id}`),
  submit: (id, data) => request.post(`/evaluations/${id}/submit`, data),
  getItems: (id) => request.get(`/evaluations/${id}/items`),
  checkSubmitted: (id) => request.get(`/evaluations/${id}/check-submitted`),
  getResults: (id) => request.get(`/evaluations/${id}/results`)
}

/** 问卷 API */
export const surveyApi = {
  list: (params) => request.get('/surveys', { params }),
  detail: (id) => request.get(`/surveys/${id}`),
  create: (data) => request.post('/surveys', data),
  update: (id, data) => request.put(`/surveys/${id}`, data),
  delete: (id) => request.delete(`/surveys/${id}`),
  getQuestions: (id) => request.get(`/surveys/${id}/questions`),
  addQuestion: (id, data) => request.post(`/surveys/${id}/questions`, data),
  updateQuestion: (id, qid, data) => request.put(`/surveys/${id}/questions/${qid}`, data),
  deleteQuestion: (id, qid) => request.delete(`/surveys/${id}/questions/${qid}`),
  submit: (id, data) => request.post(`/surveys/${id}/submit`, data),
  getMySubmission: (id) => request.get(`/surveys/${id}/my-submission`),
  getStats: (id) => request.get(`/surveys/${id}/stats`),
  getResponses: (id) => request.get(`/surveys/${id}/responses`),
  getAdminStats: () => request.get('/surveys/admin/overview')
}

/** 知识库 API */
export const knowledgeApi = {
  listArticles: (params) => request.get('/knowledge/articles', { params }),
  getArticle: (id) => request.get(`/knowledge/articles/${id}`),
  createArticle: (data) => request.post('/knowledge/articles', data),
  listNotes: (params) => request.get('/notes', { params }),
  createNote: (data) => request.post('/notes', data),
  updateNote: (id, data) => request.put(`/notes/${id}`, data),
  deleteNote: (id) => request.delete(`/notes/${id}`),
  togglePublic: (id) => request.put(`/notes/${id}/public`)
}

/** 通知 API */
export const notificationApi = {
  list: (params) => request.get('/notifications', { params }),
  detail: (id) => request.get(`/notifications/${id}`),
  unreadCount: () => request.get('/notifications/unread-count'),
  markRead: (id) => request.put(`/notifications/${id}/read`),
  markAllRead: () => request.put('/notifications/read-all'),
  send: (data) => request.post('/notifications', data),
  deleteNotify: (id) => request.delete(`/notifications/${id}`),
  clearAll: () => request.delete('/notifications/all')
}

/** 公告 API */
export const announcementApi = {
  list: (params) => request.get('/announcements', { params }),
  detail: (id) => request.get(`/announcements/${id}`),
  create: (data) => request.post('/announcements', data),
  update: (id, data) => request.put(`/announcements/${id}`, data),
  delete: (id) => request.delete(`/announcements/${id}`),
  confirm: (id) => request.post(`/announcements/${id}/confirm`),
  getConfirms: (id, params) => request.get(`/announcements/${id}/confirms`, { params })
}

/** 管理端操作日志 API */
export const operationLogApi = {
  list: (params) => request.get('/admin/operation-logs', { params }),
  overview: () => request.get('/admin/operation-logs/overview')
}

/** 文件上传 API */
export const uploadApi = {
  uploadFile: (formData) => request.post('/upload/file', formData),
  preview: (params) => request.get('/upload/preview', { params }),
  pdfPages: (params) => request.get('/upload/pdf-pages', { params })
}

/** 私信 API */
export const messageApi = {
  conversations: (params) => request.get('/messages/conversations', { params }),
  messages: (otherUserId, params) => request.get(`/messages/with/${otherUserId}`, { params }),
  send: (data) => request.post('/messages', data),
  unreadTotal: () => request.get('/messages/unread-total'),
  contacts: (params) => request.get('/messages/contacts', { params }),
  /** 清空与某人的聊天记录 */
  clearConversation: (otherUserId) => request.delete(`/messages/with/${otherUserId}`)
}
