export interface UserDto {
  id: number;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN' | 'RECRUITER';
  eloRating: number;
  skillLevel: string;
  totalInterviews: number;
  totalQuestionsSolved: number;
  avgScore: number;
  college: string;
  bio: string;
  preferredLanguage: string;
  themePreference?: 'light' | 'dark';
  hasCompletedAssessment?: boolean;
}

export interface CompanyDto {
  id: number;
  name: string;
  logoEmoji: string;
  difficultyThreshold: number;
  tags: string;
  requiredTopics: Record<string, number>;
  targeted: boolean;
  readinessScore: number | null;
}

export interface SkillGapDto {
  topic: string;
  requiredProficiency: number;
  actualProficiency: number;
  gapSeverity: number;
}

export interface RoadmapItemDto {
  id: number;
  topic: string;
  questionId: number | null;
  questionTitle: string | null;
  priorityRank: number;
  status: string;
  reason: string;
}

export interface InterviewDnaDto {
  traits: {
    status?: string;
    speed_profile?: string;
    persistence_profile?: string;
    avg_hints_per_question?: number;
    solve_rate_percent?: number;
    code_quality_trend?: string;
    questions_attempted?: number;
    favorite_topics?: string[];
  };
  narrativeText: string;
  computedAt: string;
}

export interface ResumeDto {
  rawText: string;
  parsedSkills: string[];
  experienceLevel: string;
  uploadedAt: string;
  atsScore?: number;
  analysis?: {
    experienceLevel?: string;
    yearsOfExperience?: number;
    technicalSkills?: string[];
    missingSkills?: string[];
    atsScore?: number;
    improvements?: string[];
    companySpecificTips?: Record<string, string>;
    summary?: string;
  };
}

export interface ReadinessEntry {
  companyId: number;
  companyName: string;
  logoEmoji: string;
  readinessScore: number;
  breakdown: { eloScore: number; topicScore: number; userElo: number; requiredElo: number };
}

export interface TestCase {
  args?: unknown[];
  call_expr?: string;
  expected_output?: unknown;
  description?: string;
  hidden?: boolean;
}

export interface QuestionDto {
  id: number;
  title: string;
  description: string;
  difficulty: string;
  topic: string;
  subtopic: string;
  eloRating: number;
  starterCode: string;
  starterCodeByLang?: Record<string, string>;
  supportedLanguages?: string[];
  functionSignature: string;
  hints: string[];
  testCases: TestCase[];
  constraints: string;
  examples: string;
  timeComplexity: string;
  spaceComplexity: string;
  tags: string[];
  successRate: number;
  timesAttempted: number;
}

export interface InterviewDto {
  id: number;
  status: string;
  interviewType: string;
  selectedTopic: string;
  totalQuestions: number;
  questionsAnswered: number;
  score: number;
  maxScore: number;
  scorePercent: number;
  eloBefore: number;
  eloAfter: number;
  startedAt: string;
  completedAt: string | null;
  durationMinutes: number;
}

export interface NextQuestionResponse {
  completed: boolean;
  interviewQuestionId?: number;
  questionNumber?: number;
  totalQuestions?: number;
  question?: QuestionDto;
  timeLimitSeconds?: number;
}

export interface TestResultDto {
  testNum: number;
  passed: boolean;
  description: string;
  error: string | null;
  expected: unknown;
  actual: unknown;
  hidden: boolean;
}

export interface RunTestResult {
  testNum: number;
  passed: boolean;
  description?: string;
  error?: string | null;
  stdout?: string;
}

export interface CodeReview {
  overall_assessment: string;
  score_out_of_10: number;
  strengths: string[];
  improvements: string[];
  time_complexity: string;
  space_complexity: string;
  code_quality: { readability: number; efficiency: number; correctness: number; pythonic_style: number };
  optimized_approach: string;
  key_concepts: string[];
  interview_tip: string;
}

export interface SubmitResponse {
  success: boolean;
  testResults: TestResultDto[];
  passed: number;
  total: number;
  allPassed: boolean;
  score: number;
  maxScore?: number;
  eloChange: number;
  review: CodeReview;
  analysis: unknown;
}

export interface TopicPerfDto {
  topic: string;
  proficiency: number;
  avgScore: number;
  questionsAttempted: number;
  questionsSolved: number;
  successRate: number;
}

export interface DashboardResponse {
  user: UserDto;
  recentInterviews: InterviewDto[];
  topicPerfs: TopicPerfDto[];
  chartData: { labels: string[]; scores: number[]; elos: number[] };
  radarData: { topics: string[]; values: number[] };
  streak: number;
  rank: number;
}

export interface ProfileResponse {
  user: UserDto;
  topicPerfs: TopicPerfDto[];
  allInterviews: InterviewDto[];
  aiInsights: {
    summary: string;
    strengths?: string[];
    focus_areas?: string[];
    next_steps?: string[];
    predicted_level?: string;
  } | null;
  rank: number;
}

export interface LeaderboardEntry {
  rank: number;
  name: string;
  elo: number;
  level: string;
  interviews: number;
  college: string;
}

export interface ResultsResponse {
  interview: InterviewDto;
  eloChange: number;
  aiSummary?: {
    overall_assessment?: string;
    hire_recommendation?: string;
    strengths?: string[];
    weaknesses?: string[];
    improvements?: string[];
    sample_answers?: { question: string; approach: string }[];
  } | null;
  questions: {
    iqId: number;
    question: QuestionDto;
    userCode: string;
    score: number;
    maxScore: number;
    testsPassed: number;
    testsTotal: number;
    hintsUsed: number;
    status: string;
    review: CodeReview | null;
  }[];
}

export interface ReplayEventDto {
  id: number;
  eventType: 'run' | 'submit' | 'hint';
  codeSnapshot: string;
  payload: Record<string, unknown>;
  createdAt: string;
}

export interface PlacementScoreDto {
  overallScore: number;
  breakdown: {
    eloComponent: number;
    readinessComponent: number;
    consistencyComponent: number;
    qualityComponent: number;
    interviewsCompleted: number;
    questionsAttempted?: number;
    confidence?: string;
    formula?: string;
  };
  computedAt: string;
}

export interface CandidateDto {
  userId: number;
  name: string;
  email: string;
  college: string;
  eloRating: number;
  skillLevel: string;
  placementScore: number | null;
  breakdown: Record<string, number> | null;
  shortlisted: boolean;
  note: string;
}
