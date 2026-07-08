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
  getStudents: (id, params) => request.get(`/courses/${id}/students`, { params }),
  getResources: (id, params) => request.get(`/courses/${id}/resources`, { params }),
  enroll: (id) => request.post(`/courses/${id}/enroll`),
  unenroll: (id) => request.delete(`/courses/${id}/enroll`)
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
  aiGenerate: (params) => request.post('/papers/ai-generate', null, { params }),
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

/** 作业 API */
export const assignmentApi = {
  list: (params) => request.get('/assignments', { params }),
  myList: (params) => request.get('/assignments/student/my', { params }),
  create: (data) => request.post('/assignments', data),
  detail: (id) => request.get(`/assignments/${id}`),
  update: (id, data) => request.put(`/assignments/${id}`, data),
  delete: (id) => request.delete(`/assignments/${id}`),
  submit: (id, data) => request.post(`/assignments/${id}/submit`, null, { params: data }),
  getSubmitList: (id, params) => request.get(`/assignments/${id}/submits`, { params }),
  grade: (submitId, params) => request.put(`/assignments/submits/${submitId}/grade`, null, { params })
}

/** 成绩 API */
export const gradeApi = {
  studentGrades: (studentId) => request.get(`/grades/student/${studentId}`),
  myGrades: () => request.get('/grades/my'),
  courseGrades: (courseId) => request.get(`/grades/course/${courseId}`),
  courseStats: (courseId) => request.get(`/grades/course/${courseId}/stats`)
}

/** 实训 API */
export const trainingApi = {
  listProjects: (params) => request.get('/trainings', { params }),
  createProject: (data) => request.post('/trainings', data),
  updateProject: (id, data) => request.put(`/trainings/${id}`, data),
  createTask: (projectId, data) => request.post(`/trainings/${projectId}/tasks`, data),
  listTasks: (projectId, params) => request.get(`/trainings/${projectId}/tasks`, { params }),
  submitReport: (taskId, params) => request.post(`/trainings/tasks/${taskId}/report`, null, { params }),
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
  create: (data) => request.post('/evaluations', data),
  submit: (id, data) => request.post(`/evaluations/${id}/submit`, data),
  getResults: (id) => request.get(`/evaluations/${id}/results`)
}

/** 问卷 API */
export const surveyApi = {
  list: (params) => request.get('/surveys', { params }),
  create: (data) => request.post('/surveys', data),
  submit: (id, data) => request.post(`/surveys/${id}/submit`, data),
  getQuestions: (id) => request.get(`/surveys/${id}/questions`),
  getStats: (id) => request.get(`/surveys/${id}/stats`)
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
  unreadCount: () => request.get('/notifications/unread-count'),
  markRead: (id) => request.put(`/notifications/${id}/read`),
  markAllRead: () => request.put('/notifications/read-all'),
  send: (data) => request.post('/notifications', data)
}
