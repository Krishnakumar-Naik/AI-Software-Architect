import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import GraphViewer from '../components/GraphViewer';
import { 
  Cpu, 
  Database, 
  Layers, 
  ShieldCheck,
  AlertTriangle,
  FileText,
  Download,
  Home
} from 'lucide-react';

interface SummaryData {
  totalClasses: number;
  totalServices: number;
  totalControllers: number;
  totalRepositories: number;
  totalEndpoints: number;
  dependencyCount: number;
  layeredArchitectureType: string;
  hasCircularDependencies: boolean;
  circularDependencies: Array<string[]>;
  mostConnectedComponent: string;
  mostConnectedComponentDegree: number;
  largestModule: string;
  largestModuleSize: number;
  healthScore: number;
  technicalDebt: string;
  telemetryReport: string;
  healthScoreFactors: string[];
}

interface ReportsData {
  architecture?: string;
  techDebt?: string;
  dependency?: string;
  security?: string;
}

const PublicSharePage: React.FC = () => {
  const { projectId: routeProjectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  
  // Default to project 1 if accessing /demo
  const projectId = routeProjectId ? Number(routeProjectId) : 1;

  const [activeTab, setActiveTab] = useState<'graph' | 'docs' | 'reports'>('graph');
  const [selectedNode, setSelectedNode] = useState<any>(null);

  // 1. Fetch public graph elements
  const { data: graph, isLoading: graphLoading } = useQuery<any>({
    queryKey: ['publicGraph', projectId],
    queryFn: async () => {
      const response = await api.get<any>(`/api/public/projects/${projectId}/graph`);
      return response.data;
    }
  });

  // 2. Fetch public summary metrics
  const { data: summary, isLoading: summaryLoading, isError: summaryError } = useQuery<SummaryData>({
    queryKey: ['publicSummary', projectId],
    queryFn: async () => {
      const response = await api.get<SummaryData>(`/api/public/projects/${projectId}/architecture-summary`);
      return response.data;
    }
  });

  // 3. Fetch public documentation
  const { data: docs } = useQuery<{ markdown: string }>({
    queryKey: ['publicDocumentation', projectId],
    queryFn: async () => {
      const response = await api.get<{ markdown: string }>(`/api/public/projects/${projectId}/documentation`);
      return response.data;
    }
  });

  // 4. Fetch public engineering reports
  const { data: reports } = useQuery<ReportsData>({
    queryKey: ['publicReports', projectId],
    queryFn: async () => {
      const response = await api.get<ReportsData>(`/api/public/projects/${projectId}/reports`);
      return response.data;
    }
  });

  const getTechDebtColor = (level: string) => {
    switch (level?.toUpperCase()) {
      case 'HIGH':
        return 'text-rose-500 bg-rose-500/10 border-rose-500/25';
      case 'MEDIUM':
        return 'text-amber-500 bg-amber-500/10 border-amber-500/25';
      case 'LOW':
      default:
        return 'text-emerald-500 bg-emerald-500/10 border-emerald-500/25';
    }
  };

  const getHealthScoreColor = (score: number) => {
    if (score >= 85) return 'text-emerald-400';
    if (score >= 65) return 'text-amber-400';
    return 'text-rose-400';
  };

  const handleDownloadPdfDocs = () => {
    window.open(`${api.defaults.baseURL}/api/public/projects/${projectId}/documentation/pdf`, '_blank');
  };

  const handleDownloadPdfReports = () => {
    window.open(`${api.defaults.baseURL}/api/public/projects/${projectId}/reports/pdf`, '_blank');
  };

  const isLoading = graphLoading || summaryLoading;

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col overflow-hidden">
      {/* Header toolbar */}
      <header className="h-16 border-b border-border flex items-center justify-between px-8 bg-background/50 backdrop-blur-sm relative z-10 shrink-0">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/')}
            className="p-1.5 rounded-lg border border-border bg-card/65 hover:bg-card text-gray-400 hover:text-white transition"
          >
            <Home className="h-4 w-4" />
          </button>
          <div>
            <span className="text-xs text-gray-500 font-mono">Public Portal / Showcase Mode</span>
            <h2 className="text-sm font-bold text-white tracking-tight leading-none mt-1">
              {projectId === 1 ? 'Demo Showcase Codebase' : `Shared Project #${projectId}`}
            </h2>
          </div>
        </div>

        {/* Tab switcher */}
        <div className="flex bg-secondary/10 p-1 rounded-xl border border-border/40">
          <button
            onClick={() => setActiveTab('graph')}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'graph' ? 'bg-primary text-white' : 'text-gray-400 hover:text-white'
            }`}
          >
            Architecture Graph
          </button>
          <button
            onClick={() => setActiveTab('docs')}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'docs' ? 'bg-primary text-white' : 'text-gray-400 hover:text-white'
            }`}
          >
            Documentation
          </button>
          <button
            onClick={() => setActiveTab('reports')}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition ${
              activeTab === 'reports' ? 'bg-primary text-white' : 'text-gray-400 hover:text-white'
            }`}
          >
            Engineering Reports
          </button>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/login')}
            className="px-4 py-1.5 rounded-lg border border-border bg-card hover:bg-card/80 text-xs font-semibold text-white transition"
          >
            Log In / Register
          </button>
        </div>
      </header>

      {/* Main Panel */}
      <main className="flex-1 overflow-hidden flex flex-col bg-grid relative">
        <div className="absolute top-0 right-0 w-[30%] h-[30%] rounded-full bg-primary/5 blur-[120px] pointer-events-none"></div>

        {isLoading ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="flex flex-col items-center gap-4">
              <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
              <p className="text-sm text-muted-foreground animate-pulse">Assembling public repository view...</p>
            </div>
          </div>
        ) : summaryError ? (
          <div className="flex-1 flex items-center justify-center p-6">
            <div className="glass-card p-8 rounded-2xl border border-rose-500/20 text-center max-w-xl">
              <AlertTriangle className="h-12 w-12 text-rose-500 mx-auto mb-4" />
              <h2 className="text-xl font-bold text-white mb-2">Project Not Found or Unshared</h2>
              <p className="text-gray-400 text-sm mb-6">This repository is private or does not exist.</p>
              <button
                onClick={() => navigate('/')}
                className="px-6 py-2.5 rounded-lg bg-primary hover:bg-primary/95 text-white font-medium text-sm transition"
              >
                Return Home
              </button>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col overflow-hidden">
            {activeTab === 'graph' && (
              <div className="flex-1 flex overflow-hidden">
                {/* Center Canvas Area */}
                <div className="flex-1 relative overflow-hidden bg-[#090d26]">
                  {graph && (
                    <GraphViewer graphData={graph} onSelectNode={setSelectedNode} />
                  )}
                </div>

                {/* Right Side - Node Inspector panel */}
                <div className="w-80 border-l border-border bg-card/15 flex flex-col overflow-hidden shrink-0">
                  <div className="p-4 border-b border-border bg-card/25 flex items-center gap-2">
                    <Cpu className="h-4 w-4 text-primary" />
                    <span className="text-xs font-bold text-white uppercase tracking-wider">Public Inspector</span>
                  </div>
                  <div className="flex-1 overflow-y-auto p-6 space-y-6">
                    {selectedNode ? (
                      <div className="space-y-6">
                        <div className="p-4 rounded-xl border border-border bg-card/40">
                          <span className={`inline-flex px-2 py-0.5 rounded text-[9px] font-bold uppercase tracking-wider mb-2 ${
                            selectedNode.type === 'ENDPOINT' ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20' :
                            selectedNode.type === 'CONTROLLER' ? 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20' :
                            selectedNode.type === 'SERVICE' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                            selectedNode.type === 'REPOSITORY' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' :
                            'bg-slate-500/10 text-slate-400 border border-slate-500/20'
                          }`}>
                            {selectedNode.type}
                          </span>
                          <h4 className="text-sm font-bold text-white font-mono break-all">{selectedNode.label}</h4>
                        </div>

                        {selectedNode.type === 'ENDPOINT' && (
                          <div className="space-y-4">
                            <div>
                              <h5 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Endpoint URL</h5>
                              <pre className="p-2 rounded-lg bg-background border border-border text-[11px] font-mono text-white truncate">{selectedNode.metadata.path}</pre>
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="h-full flex flex-col items-center justify-center text-center text-gray-500 pt-24">
                        <Cpu className="h-10 w-10 text-gray-600 mb-2" />
                        <span className="text-xs">Click a node on the canvas to inspect metadata</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'docs' && (
              <div className="flex-1 overflow-y-auto p-8 max-w-4xl mx-auto w-full space-y-6 select-text">
                <div className="flex items-center justify-between border-b border-border pb-4">
                  <h3 className="text-lg font-bold text-white flex items-center gap-2">
                    <FileText className="h-5 w-5 text-primary" /> Codebase Documentation Suite
                  </h3>
                  <button
                    onClick={handleDownloadPdfDocs}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-lg bg-primary hover:bg-primary/95 text-xs font-semibold text-white transition shadow-lg"
                  >
                    <Download className="h-4 w-4" /> Download PDF
                  </button>
                </div>

                <div className="prose prose-invert max-w-none text-xs leading-relaxed text-gray-300 whitespace-pre-wrap font-sans bg-card/10 border border-border p-6 rounded-2xl">
                  {docs?.markdown || 'AI is generating documentation blocks...'}
                </div>
              </div>
            )}

            {activeTab === 'reports' && (
              <div className="flex-1 overflow-y-auto p-8 max-w-4xl mx-auto w-full space-y-8 select-text">
                <div className="flex items-center justify-between border-b border-border pb-4">
                  <h3 className="text-lg font-bold text-white flex items-center gap-2">
                    <ShieldCheck className="h-5 w-5 text-emerald-400" /> Engineering Audit Reports
                  </h3>
                  <button
                    onClick={handleDownloadPdfReports}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold text-white transition shadow-lg"
                  >
                    <Download className="h-4 w-4" /> Export Audit PDF
                  </button>
                </div>

                <div className="grid grid-cols-1 gap-6">
                  <div className="p-6 rounded-2xl border border-border bg-card/20 space-y-2">
                    <h4 className="text-sm font-bold text-white">1. Architecture Analysis</h4>
                    <p className="text-xs text-gray-400 whitespace-pre-wrap leading-relaxed">
                      {reports?.architecture || 'Generating architecture review...'}
                    </p>
                  </div>

                  <div className="p-6 rounded-2xl border border-border bg-card/20 space-y-2">
                    <h4 className="text-sm font-bold text-white">2. Technical Debt & Code Smells</h4>
                    <p className="text-xs text-gray-400 whitespace-pre-wrap leading-relaxed">
                      {reports?.techDebt || 'Analyzing codebase complexity...'}
                    </p>
                  </div>

                  <div className="p-6 rounded-2xl border border-border bg-card/20 space-y-2">
                    <h4 className="text-sm font-bold text-white">3. Dependency Analysis</h4>
                    <p className="text-xs text-gray-400 whitespace-pre-wrap leading-relaxed">
                      {reports?.dependency || 'Compiling dependency couplings...'}
                    </p>
                  </div>

                  <div className="p-6 rounded-2xl border border-border bg-card/20 space-y-2">
                    <h4 className="text-sm font-bold text-white">4. Security Exposure Analysis</h4>
                    <p className="text-xs text-gray-400 whitespace-pre-wrap leading-relaxed">
                      {reports?.security || 'Auditing REST route safety...'}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Bottom Panel (Dashboard Summary info visible only on Graph Tab) */}
            {activeTab === 'graph' && (
              <div className="h-48 border-t border-border bg-card/25 flex shrink-0 overflow-hidden">
                {/* Quality & Score Column */}
                <div className="w-72 border-r border-border p-6 flex flex-col justify-between shrink-0">
                  <div>
                    <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-2">Architectural Quality</h4>
                    <div className="flex items-center gap-4">
                      {/* Score */}
                      <div className="flex flex-col">
                        <span className={`text-3xl font-extrabold font-mono ${getHealthScoreColor(summary?.healthScore || 0)}`}>
                          {summary?.healthScore}/100
                        </span>
                        <span className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Health Score</span>
                      </div>
                      {/* Tech Debt Badge */}
                      <div className="flex flex-col">
                        <span className={`inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold border ${getTechDebtColor(summary?.technicalDebt || '')}`}>
                          {summary?.technicalDebt}
                        </span>
                        <span className="text-[9px] text-gray-500 font-semibold uppercase mt-1">Technical Debt</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Statistics Grid Column */}
                <div className="flex-1 p-6 border-r border-border overflow-y-auto">
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-3 flex items-center gap-1.5">
                    <Database className="h-4 w-4 text-primary" /> Metrics Summary
                  </h4>
                  <div className="grid grid-cols-2 md:grid-cols-6 gap-3">
                    <div className="p-2.5 rounded-xl border border-border bg-card/30 text-center">
                      <div className="text-lg font-bold text-white">{summary?.totalClasses}</div>
                      <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Classes</div>
                    </div>
                    <div className="p-2.5 rounded-xl border border-border bg-card/30 text-center">
                      <div className="text-lg font-bold text-white">{summary?.totalServices}</div>
                      <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Services</div>
                    </div>
                    <div className="p-2.5 rounded-xl border border-border bg-card/30 text-center">
                      <div className="text-lg font-bold text-white">{summary?.totalControllers}</div>
                      <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Controllers</div>
                    </div>
                    <div className="p-2.5 rounded-xl border border-border bg-card/30 text-center">
                      <div className="text-lg font-bold text-white">{summary?.totalRepositories}</div>
                      <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Repositories</div>
                    </div>
                    <div className="p-2.5 rounded-xl border border-border bg-card/30 text-center">
                      <div className="text-lg font-bold text-white">{summary?.totalEndpoints}</div>
                      <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Endpoints</div>
                    </div>
                    <div className="p-2.5 rounded-xl border border-border bg-card/30 text-center">
                      <div className="text-lg font-bold text-white">{summary?.dependencyCount}</div>
                      <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Dependencies</div>
                    </div>
                  </div>
                </div>

                {/* Audit factors */}
                <div className="w-96 p-6 overflow-y-auto shrink-0 bg-card/10">
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <Layers className="h-4 w-4 text-primary" /> Health Score Breakdown
                  </h4>
                  <div className="space-y-1">
                    {summary?.healthScoreFactors?.map((factor, index) => {
                      const parts = factor.split(':');
                      return (
                        <div key={index} className="text-[10px] text-gray-400 flex justify-between gap-2">
                          <span className="truncate">{parts[0]}</span>
                          <span className="font-mono text-gray-300 font-semibold shrink-0">{parts[1] || 'N/A'}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
};

export default PublicSharePage;
