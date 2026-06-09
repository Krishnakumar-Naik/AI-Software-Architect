import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import Sidebar from '../components/Sidebar';
import GraphViewer from '../components/GraphViewer';
import { 
  ArrowLeft, 
  ShieldCheck, 
  AlertTriangle, 
  Cpu, 
  Database,
  FileCode,
  Sparkles
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
  architectureScore: number;
  technicalDebtScore: string;
  couplingScore: number;
  complexityScore: number;
}

const ArchitectureViewerPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [selectedNode, setSelectedNode] = useState<any>(null);
  const [isShared, setIsShared] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);

  // 1. Fetch Architecture Insights Summary
  const { data: summary, isLoading: summaryLoading, isError: summaryError } = useQuery<SummaryData>({
    queryKey: ['archSummary', projectId],
    queryFn: async () => {
      const response = await api.get<SummaryData>(`/api/projects/${projectId}/architecture-summary`);
      return response.data;
    },
    enabled: !!projectId,
  });

  React.useEffect(() => {
    if (summary) {
      setIsShared((summary as any).isPublic || false);
    }
  }, [summary]);

  const handleToggleShare = async () => {
    try {
      const response = await api.post(`/api/projects/${projectId}/toggle-share`);
      setIsShared((response.data as any).isPublic);
      if ((response.data as any).isPublic) {
        setShowShareModal(true);
      }
    } catch (error) {
      console.error("Failed to toggle share", error);
      alert("Failed to update project share settings.");
    }
  };

  // 2. Fetch Graph elements (nodes & edges)
  const { data: graph, isLoading: graphLoading } = useQuery<any>({
    queryKey: ['projectGraph', projectId],
    queryFn: async () => {
      const response = await api.get<any>(`/api/projects/${projectId}/graph`);
      return response.data;
    },
    enabled: !!projectId,
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

  const isLoading = summaryLoading || graphLoading;

  return (
    <div className="min-h-screen bg-background text-foreground flex overflow-hidden">
      {/* Sidebar navigation */}
      <Sidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

      {/* Main Panel */}
      <main className="flex-1 overflow-hidden flex flex-col bg-grid relative">
        <div className="absolute top-0 right-0 w-[30%] h-[30%] rounded-full bg-primary/5 blur-[120px] pointer-events-none"></div>

        {/* Header toolbar */}
        <div className="h-16 border-b border-border flex items-center justify-between px-8 relative z-10 shrink-0 bg-background/30 backdrop-blur-sm">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/dashboard')}
              className="p-1.5 rounded-lg border border-border bg-card/65 hover:bg-card text-gray-400 hover:text-white transition"
            >
              <ArrowLeft className="h-4 w-4" />
            </button>
            <div>
              <span className="text-xs text-gray-500 font-mono">Telemetry / Visualizer</span>
              <h2 className="text-sm font-bold text-white tracking-tight leading-none mt-1">Knowledge Graph Workspace</h2>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={handleToggleShare}
              className={`flex items-center gap-1.5 px-4 py-1.5 rounded-lg border text-xs font-semibold transition ${
                isShared 
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/25 hover:bg-emerald-500/20' 
                  : 'bg-card border-border text-gray-300 hover:text-white hover:bg-card/85'
              }`}
            >
              <FileCode className="h-4 w-4" /> {isShared ? 'Shared (Public)' : 'Share Link'}
            </button>
            <button
              onClick={() => navigate(`/projects/${projectId}/assistant`)}
              className="flex items-center gap-1.5 px-4 py-1.5 rounded-lg bg-primary hover:bg-primary/95 text-xs font-semibold text-white transition shadow-lg shadow-primary/20"
            >
              <Sparkles className="h-4 w-4" /> AI Architect Assistant
            </button>
          </div>
        </div>

        {isLoading ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="flex flex-col items-center gap-4">
              <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
              <p className="text-sm text-muted-foreground animate-pulse">Computing telemetry coordinates...</p>
            </div>
          </div>
        ) : summaryError ? (
          <div className="flex-1 flex items-center justify-center p-6">
            <div className="glass-card p-8 rounded-2xl border border-rose-500/20 text-center max-w-xl">
              <AlertTriangle className="h-12 w-12 text-rose-500 mx-auto mb-4" />
              <h2 className="text-xl font-bold text-white mb-2">Failed to retrieve architecture summary</h2>
              <p className="text-gray-400 text-sm mb-6">Make sure the repository contains supported source files and the analysis completed successfully.</p>
              <button
                onClick={() => navigate('/upload')}
                className="px-6 py-2.5 rounded-lg bg-primary hover:bg-primary/95 text-white font-medium text-sm transition"
              >
                Ingest Codebase
              </button>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col overflow-hidden">
            {/* Top Section - Split Workspace (Graph + Inspector Sidebar) */}
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
                  <span className="text-xs font-bold text-white uppercase tracking-wider">Node Details Inspector</span>
                </div>
                <div className="flex-1 overflow-y-auto p-6 space-y-6">
                  {selectedNode ? (
                    <div className="space-y-6">
                      {/* Name Card */}
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
                        <h4 className="text-sm font-bold text-white font-mono word-break-all break-all">{selectedNode.label}</h4>
                        {selectedNode.metadata?.package && (
                          <p className="text-[10px] text-gray-500 font-mono mt-1">{selectedNode.metadata.package}</p>
                        )}
                      </div>

                      {/* Connection counts */}
                      {selectedNode.metadata?.degree !== undefined && (
                        <div className="grid grid-cols-2 gap-3">
                          <div className="p-3 rounded-lg border border-border bg-card/10 text-center">
                            <span className="text-lg font-bold text-white">{selectedNode.metadata.degree}</span>
                            <p className="text-[9px] text-gray-500 uppercase font-semibold mt-0.5">Coupling Degree</p>
                          </div>
                          <div className="p-3 rounded-lg border border-border bg-card/10 text-center">
                            <span className="text-lg font-bold text-white">{selectedNode.metadata.methods?.length || 0}</span>
                            <p className="text-[9px] text-gray-500 uppercase font-semibold mt-0.5">Functions</p>
                          </div>
                        </div>
                      )}

                      {/* Endpoint parameters */}
                      {selectedNode.type === 'ENDPOINT' && (
                        <div className="space-y-4">
                          <div className="border-t border-border pt-4">
                            <h5 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Endpoint URL</h5>
                            <pre className="p-2 rounded-lg bg-background border border-border text-[11px] font-mono text-white truncate">{selectedNode.metadata.path}</pre>
                          </div>
                          <div>
                            <h5 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Controller Source</h5>
                            <div className="flex items-center gap-1.5 text-xs text-gray-300 font-mono">
                              <FileCode className="h-4 w-4 text-gray-500" />
                              <span>{selectedNode.metadata.controller}</span>
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Methods list */}
                      {selectedNode.metadata?.methods && selectedNode.metadata.methods.length > 0 && (
                        <div className="border-t border-border pt-4">
                          <h5 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Functions Exposed</h5>
                          <ul className="space-y-1.5">
                            {selectedNode.metadata.methods.map((m: string) => (
                              <li key={m} className="p-2 rounded-lg border border-border/40 bg-card/5 font-mono text-[10px] text-gray-300">
                                {m}()
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="h-full flex flex-col items-center justify-center text-center text-gray-500 pt-24">
                      <Cpu className="h-10 w-10 text-gray-600 mb-2" />
                      <span className="text-xs">Click a node on the graph canvas to inspect parameters</span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Expandable Bottom Panel - Architecture Insights Dashboard */}
            <div className="h-64 border-t border-border bg-card/25 flex shrink-0 overflow-hidden">
              {/* Quality & Score Column */}
              <div className="w-72 border-r border-border p-6 flex flex-col justify-between shrink-0">
                <div>
                  <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-3">Architectural Quality</h4>
                  <div className="flex items-center gap-4">
                    {/* Score */}
                    <div className="flex flex-col">
                      <span className={`text-4xl font-extrabold font-mono ${getHealthScoreColor(summary?.healthScore || 0)}`}>
                        {summary?.healthScore}
                      </span>
                      <span className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Health Score</span>
                    </div>
                    {/* Tech Debt Badge */}
                    <div className="flex flex-col">
                      <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-bold border ${getTechDebtColor(summary?.technicalDebt || '')}`}>
                        {summary?.technicalDebt}
                      </span>
                      <span className="text-[9px] text-gray-500 font-semibold uppercase mt-1.5">Technical Debt</span>
                    </div>
                  </div>
                </div>

                {summary?.healthScoreFactors && summary.healthScoreFactors.length > 0 ? (
                  <div className="mt-4 space-y-1.5 border-t border-border/40 pt-4">
                    <span className="text-[9px] text-gray-400 font-bold uppercase tracking-wider block mb-1">Health Factors</span>
                    {summary.healthScoreFactors.map((factor, index) => {
                      const parts = factor.split(':');
                      const title = parts[0];
                      const status = parts.slice(1).join(':').trim();
                      return (
                        <div key={index} className="text-[10px] text-gray-400 flex justify-between gap-2">
                          <span className="truncate">{title}</span>
                          <span className="font-mono text-gray-300 font-semibold shrink-0">{status}</span>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-[10px] text-gray-500 leading-relaxed mt-4">
                    Calculated dynamically based on layer alignment, circular references, and coupling parameters.
                  </div>
                )}
              </div>

              {/* Statistics Grid Column */}
              <div className="flex-1 p-6 border-r border-border overflow-y-auto">
                <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-4 flex items-center gap-1.5">
                  <Database className="h-4 w-4 text-primary" /> Metrics Summary
                </h4>
                <div className="grid grid-cols-2 md:grid-cols-6 gap-4">
                  <div className="p-3.5 rounded-xl border border-border bg-card/30 text-center">
                    <div className="text-xl font-bold text-white">{summary?.totalClasses}</div>
                    <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Classes</div>
                  </div>
                  <div className="p-3.5 rounded-xl border border-border bg-card/30 text-center">
                    <div className="text-xl font-bold text-white">{summary?.totalServices}</div>
                    <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Services</div>
                  </div>
                  <div className="p-3.5 rounded-xl border border-border bg-card/30 text-center">
                    <div className="text-xl font-bold text-white">{summary?.totalControllers}</div>
                    <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Controllers</div>
                  </div>
                  <div className="p-3.5 rounded-xl border border-border bg-card/30 text-center">
                    <div className="text-xl font-bold text-white">{summary?.totalRepositories}</div>
                    <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Repositories</div>
                  </div>
                  <div className="p-3.5 rounded-xl border border-border bg-card/30 text-center">
                    <div className="text-xl font-bold text-white">{summary?.totalEndpoints}</div>
                    <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Endpoints</div>
                  </div>
                  <div className="p-3.5 rounded-xl border border-border bg-card/30 text-center">
                    <div className="text-xl font-bold text-white">{summary?.dependencyCount}</div>
                    <div className="text-[9px] text-gray-500 font-semibold uppercase mt-0.5">Dependencies</div>
                  </div>
                </div>
              </div>

              {/* Warnings and Cycles Column */}
              <div className="w-96 p-6 overflow-y-auto shrink-0 bg-card/10">
                <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-3 flex items-center gap-1.5">
                  <AlertTriangle className="h-4 w-4 text-amber-500" /> Architectural Intelligence
                </h4>
                
                <div className="space-y-4 text-xs">
                  {/* Layering type */}
                  <div className="flex justify-between items-center py-1.5 border-b border-border/40">
                    <span className="text-gray-400">Layering Alignment:</span>
                    <span className="font-semibold text-white">{summary?.layeredArchitectureType}</span>
                  </div>

                  {/* Connected Node */}
                  <div className="flex justify-between items-center py-1.5 border-b border-border/40">
                    <span className="text-gray-400">Most Active Component:</span>
                    <span className="font-mono text-white truncate max-w-[150px]" title={summary?.mostConnectedComponent}>
                      {summary?.mostConnectedComponent} ({summary?.mostConnectedComponentDegree} links)
                    </span>
                  </div>

                  {/* Largest Module */}
                  <div className="flex justify-between items-center py-1.5 border-b border-border/40">
                    <span className="text-gray-400">Largest Module Size:</span>
                    <span className="font-mono text-white truncate max-w-[150px]" title={summary?.largestModule}>
                      {summary?.largestModule} ({summary?.largestModuleSize} classes)
                    </span>
                  </div>

                  {/* Circular Dependencies cycles alert */}
                  {summary?.hasCircularDependencies ? (
                    <div className="p-2.5 rounded-lg bg-rose-500/10 border border-rose-500/20 text-[11px] text-rose-300">
                      <span className="font-bold flex items-center gap-1 mb-1">
                        <AlertTriangle className="h-3.5 w-3.5" /> Circular Loops Detected:
                      </span>
                      <div className="max-h-20 overflow-y-auto space-y-1 font-mono text-[9px] pr-1">
                        {summary.circularDependencies.map((cycle, idx) => (
                          <div key={idx} className="p-1 rounded bg-rose-500/5 truncate">
                            {cycle.join(' → ')}
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <div className="p-2.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-[11px] text-emerald-400 flex items-center gap-1.5">
                      <ShieldCheck className="h-4 w-4" /> No circular dependency cycles found.
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Suggested Public Share Modal */}
      {showShareModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-card border border-border rounded-2xl shadow-2xl p-6 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-[40%] h-[40%] rounded-full bg-primary/10 blur-[100px] pointer-events-none"></div>
            
            <h3 className="text-base font-bold text-white mb-2">Public Share Link Activated</h3>
            <p className="text-xs text-gray-400 mb-4">
              Anyone with this link can view the knowledge graph, documentation, and engineering reports of this project without registration:
            </p>

            <div className="flex gap-2 mb-6">
              <input
                type="text"
                readOnly
                value={`${window.location.origin}/share/${projectId}`}
                className="flex-1 px-3 py-2 rounded-lg border border-border bg-background text-xs text-white focus:outline-none select-all"
              />
              <button
                type="button"
                onClick={() => {
                  navigator.clipboard.writeText(`${window.location.origin}/share/${projectId}`);
                }}
                className="px-3 py-2 rounded-lg bg-primary hover:bg-primary/95 text-xs font-semibold text-white transition"
              >
                Copy
              </button>
            </div>

            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setShowShareModal(false)}
                className="px-4 py-2 rounded-lg bg-secondary hover:bg-secondary/80 text-xs font-semibold text-white transition"
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

export default ArchitectureViewerPage;
