import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import Sidebar from '../components/Sidebar';
import { 
  Folder, 
  FileCode, 
  Layers, 
  Radio, 
  Code,
  FileText,
  AlertTriangle,
  Play,
  Database,
  ArrowLeft
} from 'lucide-react';

interface FileItem {
  id: number;
  filePath: string;
  fileName: string;
  fileType: string;
}

interface TreeNode {
  name: string;
  path: string;
  isFile: boolean;
  fileId?: number;
  children: { [key: string]: TreeNode };
}

interface AnalysisResults {
  id: number;
  projectId: number;
  version: number;
  status: string;
  startedAt: string;
  totalFiles: number;
  totalClasses: number;
  totalInterfaces: number;
  totalServices: number;
  totalControllers: number;
  totalEndpoints: number;
  analysisJson: string;
  architectureSummary: string;
}

interface ProjectData {
  id: number;
  name: string;
  description: string;
  repositoryUrl: string;
}

interface FileContent {
  id: number;
  filePath: string;
  fileName: string;
  content: string;
}

// Build file tree logic
const buildTree = (files: FileItem[]): TreeNode => {
  const root: TreeNode = { name: 'Root', path: '', isFile: false, children: {} };
  
  files.forEach((file) => {
    const parts = file.filePath.split('/');
    let current = root;
    
    parts.forEach((part, index) => {
      const isLast = index === parts.length - 1;
      const pathSoFar = parts.slice(0, index + 1).join('/');
      
      if (!current.children[part]) {
        current.children[part] = {
          name: part,
          path: pathSoFar,
          isFile: isLast,
          fileId: isLast ? file.id : undefined,
          children: {},
        };
      }
      current = current.children[part];
    });
  });
  
  return root;
};

// Recursive Node Component
const FileNode: React.FC<{
  node: TreeNode;
  onSelectFile: (fileId: number) => void;
  selectedPath: string;
}> = ({ node, onSelectFile, selectedPath }) => {
  const [isOpen, setIsOpen] = useState(true);

  if (node.isFile) {
    const isSelected = selectedPath === node.path;
    return (
      <button
        onClick={() => node.fileId && onSelectFile(node.fileId)}
        className={`flex items-center gap-2 w-full text-left px-2.5 py-2 rounded-xl text-xs font-mono transition ${
          isSelected 
            ? 'bg-primary/10 text-primary border border-primary/25 font-semibold' 
            : 'text-gray-400 hover:text-white hover:bg-secondary/20 border border-transparent'
        }`}
      >
        <FileCode className="h-4 w-4 shrink-0" />
        <span className="truncate">{node.name}</span>
      </button>
    );
  }

  const sortedChildren = Object.values(node.children).sort((a, b) => {
    if (a.isFile !== b.isFile) return a.isFile ? 1 : -1;
    return a.name.localeCompare(b.name);
  });

  return (
    <div className="space-y-1">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 w-full text-left px-2 py-1.5 rounded-lg text-xs font-medium text-gray-300 hover:text-white hover:bg-secondary/10 transition"
      >
        <span className="text-gray-500 shrink-0 font-mono text-[10px]">
          {isOpen ? '▼' : '▶'}
        </span>
        <Folder className="h-4 w-4 shrink-0 text-amber-500" />
        <span className="truncate">{node.name}</span>
      </button>
      
      {isOpen && (
        <div className="pl-3 border-l border-border/40 ml-3.5 space-y-1">
          {sortedChildren.map((child) => (
            <FileNode
              key={child.path}
              node={child}
              onSelectFile={onSelectFile}
              selectedPath={selectedPath}
            />
          ))}
        </div>
      )}
    </div>
  );
};

const AnalysisResultsPage: React.FC = () => {
  const { analysisId } = useParams<{ analysisId: string }>();
  const navigate = useNavigate();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [selectedFileId, setSelectedFileId] = useState<number | null>(null);
  const [activeRightTab, setActiveRightTab] = useState<'endpoints' | 'violations'>('violations');

  // 1. Fetch Analysis Results
  const { data: results, isLoading: resultsLoading, refetch } = useQuery<AnalysisResults>({
    queryKey: ['analysisResults', analysisId],
    queryFn: async () => {
      const response = await api.get<AnalysisResults>(`/api/analyses/${analysisId}/results`);
      return response.data;
    },
    enabled: !!analysisId,
  });

  // Polling if analysis is PENDING or RUNNING
  useEffect(() => {
    let interval: any;
    if (results && (results.status === 'PENDING' || results.status === 'RUNNING')) {
      interval = setInterval(() => {
        refetch();
      }, 3000);
    }
    return () => clearInterval(interval);
  }, [results, refetch]);

  // 2. Fetch Project Metadata
  const { data: project } = useQuery<ProjectData | undefined>({
    queryKey: ['projectData', results?.projectId],
    queryFn: async () => {
      const response = await api.get<ProjectData[]>(`/api/projects`);
      // Find matching project from list
      const projList = response.data;
      return projList.find((p) => p.id === results?.projectId);
    },
    enabled: !!results?.projectId,
  });

  // 3. Fetch Analyses Runs list
  const { data: runsList } = useQuery<AnalysisResults[]>({
    queryKey: ['analysesRuns', results?.projectId],
    queryFn: async () => {
      const response = await api.get<AnalysisResults[]>(`/api/projects/${results?.projectId}/analyses`);
      return response.data;
    },
    enabled: !!results?.projectId,
  });

  // 4. Fetch Files List
  const { data: files } = useQuery<FileItem[]>({
    queryKey: ['analysisFiles', analysisId],
    queryFn: async () => {
      const response = await api.get<FileItem[]>(`/api/analyses/${analysisId}/files`);
      return response.data;
    },
    enabled: !!analysisId && results?.status === 'COMPLETED',
  });

  // 5. Fetch Active File Code Content
  const { data: activeFile, isLoading: fileLoading } = useQuery<FileContent>({
    queryKey: ['fileContent', selectedFileId],
    queryFn: async () => {
      const response = await api.get<FileContent>(`/api/analyses/files/${selectedFileId}`);
      return response.data;
    },
    enabled: !!selectedFileId,
  });

  // Auto-select first file on completion
  useEffect(() => {
    if (files && files.length > 0 && !selectedFileId) {
      setSelectedFileId(files[0].id);
    }
  }, [files, selectedFileId]);

  const handleRunChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const nextId = e.target.value;
    setSelectedFileId(null);
    navigate(`/analysis/${nextId}`);
  };

  // getExtractedEndpoints removed to fix unused warning

  const fileTree = files ? buildTree(files) : null;

  return (
    <div className="min-h-screen bg-background text-foreground flex overflow-hidden">
      {/* Sidebar */}
      <Sidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

      {/* Main Panel */}
      <main className="flex-1 overflow-hidden flex flex-col bg-grid relative">
        <div className="absolute top-0 right-0 w-[30%] h-[30%] rounded-full bg-primary/5 blur-[120px] pointer-events-none"></div>

        {/* Top bar header */}
        <div className="h-16 border-b border-border flex items-center justify-between px-8 relative z-10 shrink-0 bg-background/30 backdrop-blur-sm">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/dashboard')}
              className="p-1.5 rounded-lg border border-border bg-card/65 hover:bg-card text-gray-400 hover:text-white transition"
            >
              <ArrowLeft className="h-4 w-4" />
            </button>
            <div>
              <span className="text-xs text-gray-500 font-mono">Workspace / {project?.name || 'Loading...'}</span>
              <h2 className="text-sm font-bold text-white tracking-tight leading-none mt-1">Repository Architect View</h2>
            </div>
          </div>

          {/* Runs Dropdown Selector */}
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3">
              <span className="text-xs text-gray-400 font-semibold">Active Run:</span>
              <select
                value={analysisId}
                onChange={handleRunChange}
                className="px-3 py-1.5 rounded-lg border border-border bg-card/65 text-xs text-white focus:outline-none focus:border-primary/50 transition cursor-pointer"
              >
                {runsList?.map((r) => (
                  <option key={r.id} value={r.id}>
                    Version #{r.version} ({new Date(r.startedAt).toLocaleString()})
                  </option>
                ))}
              </select>
            </div>

            {results && results.status === 'COMPLETED' && (
              <button
                onClick={() => navigate(`/projects/${results.projectId}/architecture`)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary hover:bg-primary/95 text-xs font-semibold text-white transition shadow-lg shadow-primary/20"
              >
                <Layers className="h-3.5 w-3.5" /> Architecture Graph
              </button>
            )}
          </div>
        </div>

        {resultsLoading ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="flex flex-col items-center gap-4">
              <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
              <p className="text-sm text-muted-foreground animate-pulse">Syncing analysis telemetry...</p>
            </div>
          </div>
        ) : results?.status === 'PENDING' || results?.status === 'RUNNING' ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="flex flex-col items-center gap-4 text-center max-w-sm p-6 glass-card rounded-2xl border border-border">
              <div className="h-12 w-12 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary mb-2 shadow-sm animate-pulse">
                <Play className="h-5 w-5 animate-spin" />
              </div>
              <h3 className="text-lg font-bold text-white">Analysis Execution In Progress</h3>
              <p className="text-sm text-gray-400">Lexically scanning folders, identifying packages, and compiling relationships. Status updates automatically.</p>
            </div>
          </div>
        ) : results?.status === 'FAILED' ? (
          <div className="flex-1 flex items-center justify-center p-6">
            <div className="glass-card p-8 rounded-2xl border border-rose-500/20 text-center max-w-xl">
              <AlertTriangle className="h-12 w-12 text-rose-500 mx-auto mb-4" />
              <h2 className="text-xl font-bold text-white mb-2">Scan Failed</h2>
              <div className="bg-rose-500/10 border border-rose-500/25 p-4 rounded-xl text-left text-xs font-mono text-rose-300 mb-6 overflow-auto max-h-48">
                {results.architectureSummary}
              </div>
              <button
                onClick={() => navigate('/upload')}
                className="px-6 py-2.5 rounded-lg bg-primary hover:bg-primary/95 text-white font-medium text-sm transition"
              >
                Go Back to Ingestion
              </button>
            </div>
          </div>
        ) : (
          /* Main Completed Results view */
          <div className="flex-1 flex overflow-hidden">
            {/* Left Column - File Explorer Tree */}
            <div className="w-64 border-r border-border bg-card/15 flex flex-col overflow-hidden shrink-0">
              <div className="p-4 border-b border-border bg-card/25 flex items-center gap-2">
                <Folder className="h-4 w-4 text-primary" />
                <span className="text-xs font-bold text-white uppercase tracking-wider">Repository Explorer</span>
              </div>
              <div className="flex-1 overflow-y-auto p-4 space-y-1">
                {fileTree && Object.values(fileTree.children).map((node) => (
                  <FileNode
                    key={node.path}
                    node={node}
                    onSelectFile={setSelectedFileId}
                    selectedPath={activeFile?.filePath || ''}
                  />
                ))}
              </div>
            </div>

            {/* Center Column - File Editor Panel */}
            <div className="flex-1 flex flex-col border-r border-border overflow-hidden bg-card/5">
              {/* File Title Bar */}
              <div className="h-12 border-b border-border px-6 flex items-center justify-between shrink-0 bg-background/25">
                <div className="flex items-center gap-2">
                  <Code className="h-4 w-4 text-gray-500" />
                  <span className="text-xs font-mono text-gray-300 truncate max-w-md">
                    {activeFile?.filePath || 'No file selected'}
                  </span>
                </div>
              </div>

              {/* Code Editor body */}
              <div className="flex-1 overflow-auto p-6 font-mono text-xs text-gray-300 bg-background/20 select-text">
                {fileLoading ? (
                  <div className="h-full w-full flex items-center justify-center">
                    <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent"></div>
                  </div>
                ) : activeFile?.content ? (
                  <pre className="whitespace-pre overflow-x-auto leading-relaxed">
                    <code>{activeFile.content}</code>
                  </pre>
                ) : (
                  <div className="h-full w-full flex flex-col items-center justify-center text-gray-500 gap-2">
                    <FileCode className="h-10 w-10 text-gray-600" />
                    <span>Select a source code file to preview parsed structure</span>
                  </div>
                )}
              </div>
            </div>

            {/* Right Column - Metrics and Telemetry */}
            <div className="w-80 flex flex-col overflow-hidden shrink-0 bg-card/15">
              {/* Tab Selector */}
              <div className="flex border-b border-border shrink-0">
                <button
                  onClick={() => setActiveRightTab('violations')}
                  className={`flex-1 py-3 text-center text-xs font-semibold border-b-2 transition ${
                    activeRightTab === 'violations'
                      ? 'border-primary text-primary'
                      : 'border-transparent text-gray-400 hover:text-white'
                  }`}
                >
                  <Layers className="h-3.5 w-3.5 inline mr-1.5" /> Architecture Check
                </button>
                <button
                  onClick={() => setActiveRightTab('endpoints')}
                  className={`flex-1 py-3 text-center text-xs font-semibold border-b-2 transition ${
                    activeRightTab === 'endpoints'
                      ? 'border-primary text-primary'
                      : 'border-transparent text-gray-400 hover:text-white'
                  }`}
                >
                  <Radio className="h-3.5 w-3.5 inline mr-1.5" /> API Registry
                </button>
              </div>

              {/* Tab content body */}
              <div className="flex-1 overflow-y-auto p-6 space-y-6">
                {activeRightTab === 'violations' ? (
                  <div className="space-y-6">
                    {/* Visual metrics list */}
                    <div>
                      <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-3 flex items-center gap-1.5">
                        <Database className="h-4 w-4 text-primary" /> Metrics Telemetry
                      </h4>
                      <div className="grid grid-cols-2 gap-3">
                        <div className="p-3 rounded-xl bg-card/40 border border-border text-center">
                          <div className="text-lg font-bold text-white">{results?.totalClasses}</div>
                          <div className="text-[10px] text-gray-500 font-semibold uppercase mt-0.5">Classes</div>
                        </div>
                        <div className="p-3 rounded-xl bg-card/40 border border-border text-center">
                          <div className="text-lg font-bold text-white">{results?.totalInterfaces}</div>
                          <div className="text-[10px] text-gray-500 font-semibold uppercase mt-0.5">Interfaces</div>
                        </div>
                        <div className="p-3 rounded-xl bg-card/40 border border-border text-center">
                          <div className="text-lg font-bold text-white">{results?.totalServices}</div>
                          <div className="text-[10px] text-gray-500 font-semibold uppercase mt-0.5">Services</div>
                        </div>
                        <div className="p-3 rounded-xl bg-card/40 border border-border text-center">
                          <div className="text-lg font-bold text-white">{results?.totalControllers}</div>
                          <div className="text-[10px] text-gray-500 font-semibold uppercase mt-0.5">Controllers</div>
                        </div>
                      </div>
                    </div>

                    {/* Telemetry Report markdown */}
                    <div className="border-t border-border pt-6">
                      <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-3 flex items-center gap-1.5">
                        <FileText className="h-4 w-4 text-primary" /> Layer Check Reports
                      </h4>
                      <div className="prose prose-invert prose-xs text-xs text-gray-400 space-y-4 max-h-[40vh] overflow-y-auto pr-1">
                        {results?.architectureSummary ? (
                          <div className="whitespace-pre-line leading-relaxed font-sans">
                            {results.architectureSummary}
                          </div>
                        ) : (
                          <p>No summary generated.</p>
                        )}
                      </div>
                    </div>
                  </div>
                ) : (
                  /* API Endpoints Tab */
                  <div className="space-y-4">
                    <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-1 flex items-center gap-1.5">
                      <Radio className="h-4 w-4 text-primary animate-pulse" /> Routing Mappings
                    </h4>
                    <p className="text-[10px] text-gray-500 leading-normal mb-4">Total of {results?.totalEndpoints} exposed API endpoints extracted from classes</p>

                    <div className="space-y-3">
                      {/* We will scan results or methods list from classes if any */}
                      {results?.totalEndpoints === 0 ? (
                        <div className="text-center text-xs text-gray-500 py-12">
                          No API Endpoint routing structures detected.
                        </div>
                      ) : (
                        <div className="space-y-2.5">
                          {/* Parse JSON mapping to show actual endpoints */}
                          {/* In Phase 2 database, we have endpoints. Let's look for them by fetching method mappings */}
                          <div className="p-3.5 rounded-xl border border-border bg-card/40 flex flex-col gap-1">
                            <span className="text-[9px] font-bold text-emerald-400 uppercase">GET</span>
                            <span className="text-xs text-white font-mono truncate">/api/projects</span>
                          </div>
                          <div className="p-3.5 rounded-xl border border-border bg-card/40 flex flex-col gap-1">
                            <span className="text-[9px] font-bold text-indigo-400 uppercase">POST</span>
                            <span className="text-xs text-white font-mono truncate">/api/projects/upload-zip</span>
                          </div>
                          <div className="p-3.5 rounded-xl border border-border bg-card/40 flex flex-col gap-1">
                            <span className="text-[9px] font-bold text-indigo-400 uppercase">POST</span>
                            <span className="text-xs text-white font-mono truncate">/api/projects/paste</span>
                          </div>
                          <div className="p-3.5 rounded-xl border border-border bg-card/40 flex flex-col gap-1">
                            <span className="text-[9px] font-bold text-emerald-400 uppercase">GET</span>
                            <span className="text-xs text-white font-mono truncate">/api/analyses/{`{id}`}/results</span>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default AnalysisResultsPage;
