import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import api from '../services/api';
import { 
  Upload, 
  FolderGit2, 
  FileCode, 
  ArrowRight, 
  Cpu, 
  AlertCircle, 
  FolderSync
} from 'lucide-react';

const UploadPage: React.FC = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState<'zip' | 'github' | 'paste'>('zip');
  
  // Commons
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  // Zip State
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // GitHub State
  const [githubUrl, setGithubUrl] = useState('');

  // Paste State
  const [fileName, setFileName] = useState('UserController.java');
  const [fileType, setFileType] = useState('JAVA');
  const [codeContent, setCodeContent] = useState('');

  const navigate = useNavigate();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!name.trim()) {
      setError('Project name is required');
      return;
    }

    setIsProcessing(true);

    try {
      if (activeTab === 'zip') {
        if (!selectedFile) {
          setError('Please select a ZIP file to upload');
          setIsProcessing(false);
          return;
        }

        const formData = new FormData();
        formData.append('name', name);
        formData.append('description', description);
        formData.append('file', selectedFile);

        const response = await api.post('/api/projects/upload-zip', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        });
        
        // Redirect to analysis results using the returned analysisId
        navigate(`/analysis/${response.data.analysisId}`);
      } else if (activeTab === 'github') {
        if (!githubUrl.trim()) {
          setError('Please provide a GitHub URL');
          setIsProcessing(false);
          return;
        }

        const response = await api.post('/api/projects/github', {
          name,
          description,
          cloneUrl: githubUrl,
        });

        navigate(`/analysis/${response.data.analysisId}`);
      } else if (activeTab === 'paste') {
        if (!codeContent.trim()) {
          setError('Please enter some code content');
          setIsProcessing(false);
          return;
        }

        const response = await api.post('/api/projects/paste', {
          name,
          description,
          fileName,
          fileType,
          codeContent,
        });

        navigate(`/analysis/${response.data.analysisId}`);
      }
    } catch (err: any) {
      console.error(err);
      if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else {
        setError('Failed to start code analysis. Please check your inputs.');
      }
      setIsProcessing(false);
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex overflow-hidden">
      {/* Sidebar */}
      <Sidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

      {/* Main Panel */}
      <main className="flex-1 overflow-y-auto bg-grid relative p-8">
        <div className="absolute top-0 right-0 w-[30%] h-[30%] rounded-full bg-primary/5 blur-[120px] pointer-events-none"></div>

        {/* Header */}
        <div className="mb-8 relative z-10">
          <h1 className="text-3xl font-bold tracking-tight text-white">Ingestion Center</h1>
          <p className="text-gray-400 text-sm mt-1">Upload codebases to build architectural schemas and telemetry</p>
        </div>

        {isProcessing ? (
          <div className="h-[60vh] w-full flex items-center justify-center relative z-10">
            <div className="flex flex-col items-center gap-4 text-center max-w-sm">
              <div className="h-12 w-12 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary mb-2 shadow-sm animate-pulse">
                <FolderSync className="h-6 w-6 animate-spin" />
              </div>
              <h2 className="text-xl font-bold text-white">Extracting & Parsing Codebase</h2>
              <p className="text-sm text-gray-500">Lexically scanning packages, matching MVC boundaries, and generating rule summaries. This can take a few seconds...</p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 relative z-10 max-w-5xl">
            {/* Input Selection Tab Card */}
            <div className="lg:col-span-2 glass-card p-8 rounded-2xl border border-border flex flex-col justify-between">
              <div>
                {/* Tab Selectors */}
                <div className="flex border-b border-border mb-6">
                  <button
                    onClick={() => { setActiveTab('zip'); setError(null); }}
                    className={`flex items-center gap-2 pb-3 px-4 font-semibold text-sm border-b-2 transition ${
                      activeTab === 'zip'
                        ? 'border-primary text-primary'
                        : 'border-transparent text-gray-400 hover:text-white'
                    }`}
                  >
                    <Upload className="h-4 w-4" /> ZIP Upload
                  </button>
                  <button
                    onClick={() => { setActiveTab('github'); setError(null); }}
                    className={`flex items-center gap-2 pb-3 px-4 font-semibold text-sm border-b-2 transition ${
                      activeTab === 'github'
                        ? 'border-primary text-primary'
                        : 'border-transparent text-gray-400 hover:text-white'
                    }`}
                  >
                    <FolderGit2 className="h-4 w-4" /> GitHub Import
                  </button>
                  <button
                    onClick={() => { setActiveTab('paste'); setError(null); }}
                    className={`flex items-center gap-2 pb-3 px-4 font-semibold text-sm border-b-2 transition ${
                      activeTab === 'paste'
                        ? 'border-primary text-primary'
                        : 'border-transparent text-gray-400 hover:text-white'
                    }`}
                  >
                    <FileCode className="h-4 w-4" /> Code Paste
                  </button>
                </div>

                {error && (
                  <div className="mb-5 p-4 rounded-lg bg-destructive/10 border border-destructive/25 text-destructive-foreground text-sm flex items-start gap-3">
                    <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
                    <span>{error}</span>
                  </div>
                )}

                <form onSubmit={handleUploadSubmit} className="space-y-6">
                  {/* Common inputs */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                        Project Name
                      </label>
                      <input
                        type="text"
                        placeholder="e.g. My E-Commerce App"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white placeholder-gray-600 focus:outline-none focus:border-primary/50 focus:ring-1 focus:ring-primary/50 transition"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                        Description
                      </label>
                      <input
                        type="text"
                        placeholder="Short overview (optional)"
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white placeholder-gray-600 focus:outline-none focus:border-primary/50 focus:ring-1 focus:ring-primary/50 transition"
                      />
                    </div>
                  </div>

                  {/* Active Tab Ingestion Content */}
                  {activeTab === 'zip' && (
                    <div className="border-2 border-dashed border-border rounded-xl p-8 flex flex-col items-center justify-center bg-card/15 hover:bg-card/25 transition relative">
                      <input
                        type="file"
                        accept=".zip"
                        onChange={handleFileChange}
                        className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                      />
                      <Upload className="h-10 w-10 text-gray-500 mb-4" />
                      {selectedFile ? (
                        <div className="text-center">
                          <p className="text-sm font-semibold text-white truncate max-w-xs">{selectedFile.name}</p>
                          <p className="text-xs text-gray-500 mt-1">{(selectedFile.size / 1024 / 1024).toFixed(2)} MB</p>
                        </div>
                      ) : (
                        <div className="text-center">
                          <p className="text-sm font-semibold text-gray-300">Click or drag ZIP archive to upload</p>
                          <p className="text-xs text-gray-500 mt-1">Accepts source code folders compressed in .zip formats</p>
                        </div>
                      )}
                    </div>
                  )}

                  {activeTab === 'github' && (
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                        GitHub Repository URL
                      </label>
                      <div className="relative">
                        <FolderGit2 className="absolute left-3.5 top-3.5 h-5 w-5 text-gray-500" />
                        <input
                          type="url"
                          placeholder="https://github.com/username/repository"
                          value={githubUrl}
                          onChange={(e) => setGithubUrl(e.target.value)}
                          className="w-full pl-11 pr-4 py-3 rounded-xl border border-border bg-card/65 text-white placeholder-gray-600 focus:outline-none focus:border-primary/50 focus:ring-1 focus:ring-primary/50 transition"
                        />
                      </div>
                    </div>
                  )}

                  {activeTab === 'paste' && (
                    <div className="space-y-4">
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                            File Name
                          </label>
                          <input
                            type="text"
                            value={fileName}
                            onChange={(e) => setFileName(e.target.value)}
                            className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white focus:outline-none focus:border-primary/50 transition"
                          />
                        </div>
                        <div>
                          <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                            Language Type
                          </label>
                          <select
                            value={fileType}
                            onChange={(e) => setFileType(e.target.value)}
                            className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white focus:outline-none focus:border-primary/50 transition"
                          >
                            <option value="JAVA">Java</option>
                            <option value="PYTHON">Python</option>
                            <option value="TYPESCRIPT">TypeScript</option>
                            <option value="JAVASCRIPT">JavaScript</option>
                          </select>
                        </div>
                      </div>
                      <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                          Source Code Editor
                        </label>
                        <textarea
                          placeholder="Paste your source file here..."
                          value={codeContent}
                          onChange={(e) => setCodeContent(e.target.value)}
                          rows={10}
                          className="w-full p-4 rounded-xl border border-border bg-card/65 text-white font-mono text-xs focus:outline-none focus:border-primary/50 transition"
                        />
                      </div>
                    </div>
                  )}

                  <button
                    type="submit"
                    className="w-full py-3.5 rounded-xl bg-primary hover:bg-primary/95 text-white font-semibold flex items-center justify-center gap-2 transition shadow-lg shadow-primary/20 hover:scale-[1.01]"
                  >
                    Run Analyzer Engine <ArrowRight className="h-5 w-5" />
                  </button>
                </form>
              </div>
            </div>

            {/* Quick Tips Column */}
            <div className="glass-card p-6 rounded-2xl border border-border h-fit">
              <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                <Cpu className="h-5 w-5 text-primary animate-pulse" /> Scanning Protocol
              </h3>
              <div className="space-y-4 text-sm text-gray-400 leading-relaxed">
                <div>
                  <h4 className="font-semibold text-white text-xs uppercase tracking-wider mb-1">Supported Code</h4>
                  <p>Standard Java (Spring Boot), Python (Flask/FastAPI), and JavaScript/TypeScript (Express/NestJS) classes are supported.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-white text-xs uppercase tracking-wider mb-1">MVC Stereotyping</h4>
                  <p>Annotations like `@RestController`, `@Service`, `@Repository`, and naming suffixes (e.g. `UserController`, `MyService`) are scanned to map boundaries.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-white text-xs uppercase tracking-wider mb-1">Clean Exclusions</h4>
                  <p>Common dependency locks (`node_modules`), caches (`.git`), or compiled items (`build/`, `target/`) are skipped automatically to protect scanner speed.</p>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default UploadPage;
