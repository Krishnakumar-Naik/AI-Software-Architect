import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import Sidebar from '../components/Sidebar';
import { 
  Send, 
  MessageSquare, 
  Copy, 
  Sparkles, 
  Cpu, 
  Layers, 
  Radio, 
  CheckCircle,
  FileCode,
  ListRestart,
  Briefcase
} from 'lucide-react';

interface ConversationItem {
  id: number;
  title: string;
  updatedAt: string;
}

interface MessageItem {
  sender: 'USER' | 'ASSISTANT';
  content: string;
  timestamp: string;
}

interface ConversationDetails {
  id: number;
  title: string;
  projectId: number;
  messages: MessageItem[];
}

interface SummaryData {
  totalClasses: number;
  totalServices: number;
  totalControllers: number;
  totalRepositories: number;
  totalEndpoints: number;
  dependencyCount: number;
  layeredArchitectureType: string;
  healthScore: number;
}

const AiAssistantPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const queryClient = useQueryClient();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState<'inspector' | 'telemetry'>('inspector');

  // Input states
  const [inputMsg, setInputMsg] = useState('');
  const [activeConvId, setActiveConvId] = useState<number | null>(null);
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);
  const [showResumeModal, setShowResumeModal] = useState(false);
  const [resumeBullets, setResumeBullets] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 1. Fetch Architecture Insights Summary for Prompt Inspector
  const { data: summary } = useQuery<SummaryData>({
    queryKey: ['archSummary', projectId],
    queryFn: async () => {
      const response = await api.get<SummaryData>(`/api/projects/${projectId}/architecture-summary`);
      return response.data;
    },
    enabled: !!projectId,
  });

  // 2. Fetch Project conversations list
  const { data: conversations, refetch: refetchConversations } = useQuery<ConversationItem[]>({
    queryKey: ['assistantConversations', projectId],
    queryFn: async () => {
      const response = await api.get<ConversationItem[]>(`/api/assistant/conversations?projectId=${projectId}`);
      return response.data;
    },
    enabled: !!projectId,
  });

  // 3. Fetch active conversation details
  const { data: activeConv } = useQuery<ConversationDetails>({
    queryKey: ['conversationDetails', activeConvId],
    queryFn: async () => {
      const response = await api.get<ConversationDetails>(`/api/assistant/conversations/${activeConvId}`);
      return response.data;
    },
    enabled: !!activeConvId,
  });

  // Scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeConv?.messages]);

  // Mutation to handle chat calls (POST /api/assistant/chat)
  const chatMutation = useMutation({
    mutationFn: async (payload: { endpoint: string; message: string }) => {
      const response = await api.post<any>(payload.endpoint, {
        projectId: Number(projectId),
        conversationId: activeConvId,
        message: payload.message
      });
      return response.data;
    },
    onSuccess: (data) => {
      setInputMsg('');
      if (!activeConvId) {
        setActiveConvId(data.conversationId);
      }
      queryClient.invalidateQueries({ queryKey: ['conversationDetails'] });
      refetchConversations();
    }
  });

  // Mutation to handle resume bullet points (POST /api/assistant/resume-bullets)
  const resumeMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post<any>(`/api/assistant/resume-bullets?projectId=${projectId}`);
      return response.data;
    },
    onSuccess: (data) => {
      setInputMsg('');
      setResumeBullets(data.bullets);
      setShowResumeModal(true);
    }
  });

  const handleSendMessage = (e?: React.FormEvent, customMsg?: string) => {
    e?.preventDefault();
    const message = customMsg || inputMsg;
    if (!message.trim() || chatMutation.isPending) return;

    chatMutation.mutate({
      endpoint: '/api/assistant/chat',
      message: message
    });
  };

  const handleShortcutClick = (category: 'explain' | 'interview' | 'review' | 'security' | 'resume') => {
    if (chatMutation.isPending) return;

    let endpoint = '/api/assistant/chat';
    let message = 'Explain the architecture of this project.';

    if (category === 'interview') {
      endpoint = '/api/assistant/interview';
      message = 'Generate technical project-based interview screening questions.';
    } else if (category === 'review') {
      endpoint = '/api/assistant/review';
      message = 'Generate a complete design architecture review focusing on tight coupling and design metrics.';
    } else if (category === 'security') {
      endpoint = '/api/assistant/security';
      message = 'Generate a comprehensive security review for the API endpoints and layers.';
    } else if (category === 'resume') {
      resumeMutation.mutate();
      return;
    }

    chatMutation.mutate({
      endpoint,
      message
    });
  };

  const handleNewConversation = () => {
    setActiveConvId(null);
  };

  const copyToClipboard = (text: string, index: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex overflow-hidden">
      {/* Sidebar navigation */}
      <Sidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

      {/* Main Panel */}
      <main className="flex-1 overflow-hidden flex bg-card/5 relative">
        {/* Left Side: Conversations History List */}
        <div className="w-64 border-r border-border bg-card/15 flex flex-col overflow-hidden shrink-0">
          <div className="p-4 border-b border-border bg-card/25 flex items-center justify-between">
            <span className="text-xs font-bold text-white uppercase tracking-wider">Chat Sessions</span>
            <button
              onClick={handleNewConversation}
              className="p-1 rounded bg-primary hover:bg-primary/95 text-white"
              title="Start New Chat"
            >
              <PlusIcon className="h-4 w-4" />
            </button>
          </div>
          
          <div className="flex-1 overflow-y-auto p-4 space-y-1.5">
            {conversations?.map((c) => (
              <button
                key={c.id}
                onClick={() => setActiveConvId(c.id)}
                className={`flex items-center gap-3 w-full text-left px-3 py-2.5 rounded-xl text-xs font-medium transition ${
                  activeConvId === c.id
                    ? 'bg-primary/10 text-primary border border-primary/25 font-semibold'
                    : 'text-gray-400 hover:text-white hover:bg-secondary/20 border border-transparent'
                }`}
              >
                <MessageSquare className="h-4 w-4 shrink-0 text-gray-500" />
                <span className="truncate">{c.title}</span>
              </button>
            ))}

            {(!conversations || conversations.length === 0) && (
              <div className="text-center text-xs text-gray-600 py-12">
                No active conversations
              </div>
            )}
          </div>
        </div>

        {/* Center Panel: Active Chat Workspace */}
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* Chat header */}
          <div className="h-16 border-b border-border flex items-center justify-between px-8 shrink-0 bg-background/25">
            <div className="flex items-center gap-3">
              <div className="h-8 w-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center text-primary">
                <Sparkles className="h-4 w-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-white leading-none">Spring AI Gemini Architect</h3>
                <p className="text-[10px] text-gray-500 mt-1">
                  {activeConv ? activeConv.title : 'New conversation session started'}
                </p>
              </div>
            </div>
            
            {activeConvId && (
              <button
                onClick={handleNewConversation}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-border bg-card/65 hover:bg-card text-xs text-gray-300 hover:text-white transition"
              >
                <ListRestart className="h-3.5 w-3.5" /> Reset Context
              </button>
            )}
          </div>

          {/* Chat logs scroll area */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-background/10">
            {/* Show prompt shortcuts if conversation is empty */}
            {(!activeConv || activeConv.messages.length === 0) && !chatMutation.isPending && (
              <div className="max-w-2xl mx-auto py-12 space-y-8 text-center">
                <div className="space-y-2">
                  <h2 className="text-2xl font-extrabold text-white">Software Architect Assistant</h2>
                  <p className="text-gray-400 text-sm">Context-grounded chatbot capable of reviewing structure, suggesting improvements, and screener prep.</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-left">
                  <button
                    onClick={() => handleShortcutClick('explain')}
                    className="p-4 rounded-xl border border-border bg-card/40 hover:bg-card hover:border-primary/30 text-left transition group"
                  >
                    <span className="text-xs font-bold text-white block mb-1">Explain Architecture</span>
                    <span className="text-[10px] text-gray-500 leading-normal">Retrieve a detailed breakdown of service layers, controller flows, and repository couplings.</span>
                  </button>
                  <button
                    onClick={() => handleShortcutClick('review')}
                    className="p-4 rounded-xl border border-border bg-card/40 hover:bg-card hover:border-primary/30 text-left transition group"
                  >
                    <span className="text-xs font-bold text-white block mb-1">Audits & Coupling Reviews</span>
                    <span className="text-[10px] text-gray-500 leading-normal">Detect layer boundary violations, code redundancies, and highly coupled classes.</span>
                  </button>
                  <button
                    onClick={() => handleShortcutClick('interview')}
                    className="p-4 rounded-xl border border-border bg-card/40 hover:bg-card hover:border-primary/30 text-left transition group"
                  >
                    <span className="text-xs font-bold text-white block mb-1">Generate Interview Questions</span>
                    <span className="text-[10px] text-gray-500 leading-normal">Build project-based system design, refactoring, and pattern screening questions.</span>
                  </button>
                  <button
                    onClick={() => handleShortcutClick('security')}
                    className="p-4 rounded-xl border border-border bg-card/40 hover:bg-card hover:border-primary/30 text-left transition group"
                  >
                    <span className="text-xs font-bold text-white block mb-1">Security Assessments</span>
                    <span className="text-[10px] text-gray-500 leading-normal">Find database threats, missing security annotations, and API exposure risks.</span>
                  </button>
                </div>

                {/* Kill feature generator */}
                <button
                  onClick={() => handleShortcutClick('resume')}
                  className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-gradient-to-r from-primary to-indigo-600 hover:from-primary/95 hover:to-indigo-600/95 font-semibold text-xs text-white transition shadow-lg shadow-primary/20 hover:scale-[1.01]"
                >
                  <Briefcase className="h-4 w-4 animate-pulse" /> Generate Resume Bullet Points
                </button>
              </div>
            )}

            {/* Conversation Logs */}
            <div className="max-w-3xl mx-auto space-y-6">
              {activeConv?.messages.map((m, idx) => (
                <div
                  key={idx}
                  className={`flex gap-4 ${
                    m.sender === 'USER' ? 'justify-end' : 'justify-start'
                  }`}
                >
                  {m.sender === 'ASSISTANT' && (
                    <div className="h-8 w-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center text-primary shrink-0">
                      <Sparkles className="h-4.5 w-4.5" />
                    </div>
                  )}

                  <div
                    className={`max-w-[85%] rounded-2xl p-4 border text-sm leading-relaxed ${
                      m.sender === 'USER'
                        ? 'bg-primary/10 border-primary/20 text-white'
                        : 'bg-card/65 border-border text-gray-300'
                    }`}
                  >
                    {/* Markdown Renderer */}
                    <div className="prose prose-invert prose-xs text-xs whitespace-pre-wrap select-text font-sans">
                      {m.content}
                    </div>

                    {m.sender === 'ASSISTANT' && (
                      <div className="flex justify-end mt-3 border-t border-border/40 pt-2 text-[10px]">
                        <button
                          onClick={() => copyToClipboard(m.content, idx)}
                          className="flex items-center gap-1 text-gray-500 hover:text-white transition"
                        >
                          <Copy className="h-3.5 w-3.5" />
                          {copiedIndex === idx ? 'Copied!' : 'Copy Response'}
                        </button>
                      </div>
                    )}
                  </div>

                  {m.sender === 'USER' && (
                    <div className="h-8 w-8 rounded-full bg-gradient-to-tr from-indigo-500 to-primary flex items-center justify-center font-bold text-white text-[11px] shrink-0">
                      US
                    </div>
                  )}
                </div>
              ))}

              {/* Streaming placeholder */}
              {chatMutation.isPending && (
                <div className="flex gap-4 justify-start">
                  <div className="h-8 w-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center text-primary shrink-0 animate-pulse">
                    <Sparkles className="h-4.5 w-4.5 animate-spin" />
                  </div>
                  <div className="max-w-[85%] rounded-2xl p-4 border bg-card/65 border-border text-gray-500 text-xs italic flex items-center gap-2">
                    <div className="h-2 w-2 rounded-full bg-primary animate-bounce" style={{ animationDelay: '0ms' }} />
                    <div className="h-2 w-2 rounded-full bg-primary animate-bounce" style={{ animationDelay: '150ms' }} />
                    <div className="h-2 w-2 rounded-full bg-primary animate-bounce" style={{ animationDelay: '300ms' }} />
                    <span className="ml-1">Grounded context assembled... calling Gemini API...</span>
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>
          </div>

          {/* Chat input footer */}
          <div className="p-4 border-t border-border bg-card/10 backdrop-blur-sm shrink-0">
            <form onSubmit={handleSendMessage} className="max-w-3xl mx-auto flex gap-3">
              <input
                type="text"
                placeholder="Ask assistant about code review, refactoring patterns, or dependency flow..."
                value={inputMsg}
                onChange={(e) => setInputMsg(e.target.value)}
                disabled={chatMutation.isPending}
                className="flex-1 px-4 py-3 rounded-xl border border-border bg-card/65 text-xs text-white placeholder-gray-600 focus:outline-none focus:border-primary/50 focus:ring-1 focus:ring-primary/50 transition disabled:opacity-50"
              />
              <button
                type="submit"
                disabled={!inputMsg.trim() || chatMutation.isPending}
                className="px-4 py-3 rounded-xl bg-primary hover:bg-primary/95 text-white disabled:opacity-50 transition shadow-lg shadow-primary/20 shrink-0"
              >
                <Send className="h-4.5 w-4.5" />
              </button>
            </form>
          </div>
        </div>

        {/* Right Panel: Context Inspector */}
        <div className="w-80 border-l border-border bg-card/15 flex flex-col overflow-hidden shrink-0">
          <div className="flex border-b border-border shrink-0">
            <button
              onClick={() => setActiveTab('inspector')}
              className={`flex-1 py-3 text-center text-[10px] font-bold uppercase tracking-wider border-b-2 transition ${
                activeTab === 'inspector'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-gray-500 hover:text-white'
              }`}
            >
              <Cpu className="h-3.5 w-3.5 inline mr-1.5" /> AI Context Preview
            </button>
            <button
              onClick={() => setActiveTab('telemetry')}
              className={`flex-1 py-3 text-center text-[10px] font-bold uppercase tracking-wider border-b-2 transition ${
                activeTab === 'telemetry'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-gray-500 hover:text-white'
              }`}
            >
              <Layers className="h-3.5 w-3.5 inline mr-1.5" /> Telemetry Summary
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {activeTab === 'inspector' ? (
              <div className="space-y-6">
                <div>
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-2">AI Context Preview</h4>
                  <p className="text-[10px] text-gray-500 leading-relaxed mb-4">Grounded repository metadata fed to the active LLM context window:</p>

                  <div className="space-y-3">
                    <div className="p-3.5 rounded-xl border border-border bg-card/40 flex justify-between items-center">
                      <div className="flex items-center gap-2">
                        <FileCode className="h-4 w-4 text-primary" />
                        <span className="text-xs text-gray-300 font-semibold">Classes Loaded:</span>
                      </div>
                      <span className="font-mono text-sm font-bold text-white">{summary?.totalClasses || 0}</span>
                    </div>

                    <div className="p-3.5 rounded-xl border border-border bg-card/40 flex justify-between items-center">
                      <div className="flex items-center gap-2">
                        <Layers className="h-4 w-4 text-primary" />
                        <span className="text-xs text-gray-300 font-semibold">Dependencies Loaded:</span>
                      </div>
                      <span className="font-mono text-sm font-bold text-white">{summary?.dependencyCount || 0}</span>
                    </div>

                    <div className="p-3.5 rounded-xl border border-border bg-card/40 flex justify-between items-center">
                      <div className="flex items-center gap-2">
                        <Radio className="h-4 w-4 text-primary" />
                        <span className="text-xs text-gray-300 font-semibold">Endpoints Loaded:</span>
                      </div>
                      <span className="font-mono text-sm font-bold text-white">{summary?.totalEndpoints || 0}</span>
                    </div>
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-primary/5 border border-primary/20 text-[10px] text-primary leading-normal flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 shrink-0 text-primary mt-0.5" />
                  <span>The assistant compiles this structured code telemetry so queries are grounded in your actual code constraints.</span>
                </div>
              </div>
            ) : (
              <div className="space-y-6">
                {/* Telemetry Summary */}
                <div>
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-3">Project Metadata</h4>
                  <div className="space-y-3.5">
                    <div className="p-3.5 rounded-xl border border-border bg-card/40">
                      <span className="text-[9px] text-gray-500 font-semibold uppercase block mb-1">Architecture Health</span>
                      <div className="text-lg font-bold text-emerald-400 font-mono">{summary?.healthScore || 0}/100</div>
                    </div>
                    <div className="p-3.5 rounded-xl border border-border bg-card/40">
                      <span className="text-[9px] text-gray-500 font-semibold uppercase block mb-1">Layering Alignment</span>
                      <div className="text-xs font-semibold text-white">{summary?.layeredArchitectureType || 'N/A'}</div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Suggested Resume Bullets Modal */}
      {showResumeModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm">
          <div className="w-full max-w-2xl bg-card border border-border rounded-2xl shadow-2xl p-6 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-[40%] h-[40%] rounded-full bg-primary/10 blur-[100px] pointer-events-none"></div>
            
            <div className="flex items-center gap-2 mb-4">
              <Briefcase className="h-5 w-5 text-primary" />
              <h3 className="text-base font-bold text-white">Suggested Resume Bullet Points</h3>
            </div>
            
            <p className="text-xs text-gray-400 mb-4">
              Below are high-impact accomplishments synthesized from your codebase analysis. You can copy these directly into your resume:
            </p>

            <div className="bg-background/50 border border-border p-4 rounded-xl text-xs font-mono text-gray-300 mb-6 overflow-y-auto max-h-[300px] whitespace-pre-wrap leading-relaxed select-text">
              {resumeBullets}
            </div>

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  navigator.clipboard.writeText(resumeBullets);
                }}
                className="px-4 py-2 rounded-lg bg-secondary hover:bg-secondary/80 text-xs font-semibold text-white transition flex items-center gap-1.5"
              >
                <Copy className="h-4 w-4" /> Copy Bullets
              </button>
              <button
                type="button"
                onClick={() => setShowResumeModal(false)}
                className="px-4 py-2 rounded-lg bg-primary hover:bg-primary/95 text-xs font-semibold text-white transition"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// Simple PlusIcon component inline to avoid extra import error
const PlusIcon = (props: React.SVGProps<SVGSVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    {...props}
  >
    <path d="M5 12h14" />
    <path d="M12 5v14" />
  </svg>
);

export default AiAssistantPage;
