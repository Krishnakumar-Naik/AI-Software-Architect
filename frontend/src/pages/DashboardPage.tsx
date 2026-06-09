import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import Sidebar from '../components/Sidebar';
import { 
  FolderGit2, 
  FilePieChart, 
  BookMarked, 
  MessageSquareCode, 
  Activity, 
  CheckCircle2, 
  XCircle, 
  Clock,
  RefreshCw,
  Server,
  Plus
} from 'lucide-react';

interface StatsResponse {
  projectsAnalyzed: number;
  architectureReports: number;
  documentationGenerated: number;
  aiConversations: number;
  recentActivities: Array<{
    id: string;
    project: string;
    action: string;
    time: string;
    status: string;
  }>;
  systemHealth: Array<{
    service: string;
    status: string;
    latencyMs: number;
  }>;
}

const DashboardPage: React.FC = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const navigate = useNavigate();

  const { data: stats, isLoading, isError, refetch, isRefetching } = useQuery<StatsResponse>({
    queryKey: ['dashboardStats'],
    queryFn: async () => {
      const response = await api.get<StatsResponse>('/api/dashboard/stats');
      return response.data;
    },
    refetchOnWindowFocus: false,
  });

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <CheckCircle2 className="h-4 w-4 text-emerald-400" />;
      case 'FAILED':
        return <XCircle className="h-4 w-4 text-rose-400" />;
      case 'IN_PROGRESS':
        return <Clock className="h-4 w-4 text-amber-400" />;
      default:
        return <Activity className="h-4 w-4 text-gray-400" />;
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20';
      case 'FAILED':
        return 'bg-rose-500/10 text-rose-400 border border-rose-500/20';
      case 'IN_PROGRESS':
        return 'bg-amber-500/10 text-amber-400 border border-amber-500/20 animate-pulse';
      default:
        return 'bg-gray-500/10 text-gray-400 border border-gray-500/20';
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex overflow-hidden">
      {/* Collapsible Sidebar */}
      <Sidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto bg-grid relative p-8">
        <div className="absolute top-0 right-0 w-[30%] h-[30%] rounded-full bg-primary/5 blur-[120px] pointer-events-none"></div>

        {/* Dashboard Header */}
        <div className="flex items-center justify-between mb-8 relative z-10">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-white">System Architecture Control</h1>
            <p className="text-gray-400 text-sm mt-1">Real-time status feed and metric telemetry logs</p>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/upload')}
              className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary hover:bg-primary/95 text-sm font-medium text-white transition shadow-lg shadow-primary/25"
            >
              <Plus className="h-4 w-4" />
              New Analysis
            </button>
            <button
              onClick={() => refetch()}
              disabled={isLoading || isRefetching}
              className="flex items-center gap-2 px-4 py-2 rounded-lg border border-border bg-card/65 hover:bg-card text-sm text-gray-300 hover:text-white transition disabled:opacity-50"
            >
              <RefreshCw className={`h-4 w-4 ${isRefetching ? 'animate-spin' : ''}`} />
              Sync Dashboard
            </button>
          </div>
        </div>

        {isLoading ? (
          <div className="h-[60vh] w-full flex items-center justify-center">
            <div className="flex flex-col items-center gap-4">
              <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
              <p className="text-sm text-muted-foreground animate-pulse">Syncing telemetry data...</p>
            </div>
          </div>
        ) : isError ? (
          <div className="glass-card p-8 rounded-2xl border border-rose-500/20 text-center max-w-xl mx-auto my-12">
            <XCircle className="h-12 w-12 text-rose-500 mx-auto mb-4" />
            <h2 className="text-xl font-bold text-white mb-2">Sync Connection Interrupted</h2>
            <p className="text-gray-400 mb-6">Failed to retrieve dashboard metric telemetry from the API server. Please check the backend connection.</p>
            <button
              onClick={() => refetch()}
              className="px-6 py-2.5 rounded-lg bg-primary hover:bg-primary/95 text-white font-medium text-sm transition"
            >
              Retry Sync
            </button>
          </div>
        ) : (
          <div className="space-y-8 relative z-10">
            {/* Telemetry Metrics Grid */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
              {/* Projects card */}
              <div className="glass-card p-6 rounded-2xl relative overflow-hidden group hover:border-primary/30 transition duration-200">
                <div className="absolute top-0 right-0 w-24 h-24 rounded-full bg-primary/5 blur-xl group-hover:bg-primary/10 transition-all duration-300"></div>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">Projects Analyzed</span>
                  <div className="p-2 rounded-lg bg-primary/10 text-primary border border-primary/20">
                    <FolderGit2 className="h-5 w-5" />
                  </div>
                </div>
                <div className="text-3xl font-bold text-white">{stats?.projectsAnalyzed}</div>
                <div className="text-xs text-emerald-400 font-medium mt-2 flex items-center gap-1">
                  <span>+18%</span> <span className="text-gray-500">since last check</span>
                </div>
              </div>

              {/* Reports card */}
              <div className="glass-card p-6 rounded-2xl relative overflow-hidden group hover:border-indigo-500/30 transition duration-200">
                <div className="absolute top-0 right-0 w-24 h-24 rounded-full bg-indigo-500/5 blur-xl group-hover:bg-indigo-500/10 transition-all duration-300"></div>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">Architecture Reports</span>
                  <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    <FilePieChart className="h-5 w-5" />
                  </div>
                </div>
                <div className="text-3xl font-bold text-white">{stats?.architectureReports}</div>
                <div className="text-xs text-indigo-400 font-medium mt-2 flex items-center gap-1">
                  <span>+3 new</span> <span className="text-gray-500">reports generated today</span>
                </div>
              </div>

              {/* Docs card */}
              <div className="glass-card p-6 rounded-2xl relative overflow-hidden group hover:border-emerald-500/30 transition duration-200">
                <div className="absolute top-0 right-0 w-24 h-24 rounded-full bg-emerald-500/5 blur-xl group-hover:bg-emerald-500/10 transition-all duration-300"></div>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">Docs Generated</span>
                  <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    <BookMarked className="h-5 w-5" />
                  </div>
                </div>
                <div className="text-3xl font-bold text-white">{stats?.documentationGenerated}</div>
                <div className="text-xs text-emerald-400 font-medium mt-2 flex items-center gap-1">
                  <span>100% updated</span> <span className="text-gray-500">to main branch</span>
                </div>
              </div>

              {/* Conversations card */}
              <div className="glass-card p-6 rounded-2xl relative overflow-hidden group hover:border-rose-500/30 transition duration-200">
                <div className="absolute top-0 right-0 w-24 h-24 rounded-full bg-rose-500/5 blur-xl group-hover:bg-rose-500/10 transition-all duration-300"></div>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">AI Conversations</span>
                  <div className="p-2 rounded-lg bg-rose-500/10 text-rose-400 border border-rose-500/20">
                    <MessageSquareCode className="h-5 w-5" />
                  </div>
                </div>
                <div className="text-3xl font-bold text-white">{stats?.aiConversations}</div>
                <div className="text-xs text-rose-400 font-medium mt-2 flex items-center gap-1">
                  <span>Gemini 2.0 Flash</span> <span className="text-gray-500">active</span>
                </div>
              </div>
            </div>

            {/* Bottom Details Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Scan Activity Table */}
              <div className="glass-card rounded-2xl border border-border p-6 lg:col-span-2">
                <h3 className="text-lg font-bold text-white mb-4">Analysis Queue & Activity</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm text-gray-300">
                    <thead>
                      <tr className="border-b border-border text-gray-400 text-xs font-semibold uppercase">
                        <th className="pb-3">Repository</th>
                        <th className="pb-3">Action</th>
                        <th className="pb-3">Timeline</th>
                        <th className="pb-3 text-right">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {stats?.recentActivities.map((act) => (
                        <tr 
                          key={act.id} 
                          className={`transition-colors ${act.id !== '0' ? 'cursor-pointer hover:bg-secondary/20' : 'hover:bg-secondary/5'}`}
                          onClick={() => act.id !== '0' && navigate(`/analysis/${act.id}`)}
                        >
                          <td className="py-3.5 font-medium text-white">{act.project}</td>
                          <td className="py-3.5 text-gray-400">{act.action}</td>
                          <td className="py-3.5 text-gray-500">{act.time}</td>
                          <td className="py-3.5 text-right">
                            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${getStatusBadgeClass(act.status)}`}>
                              {getStatusIcon(act.status)}
                              {act.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Service Health Cards */}
              <div className="glass-card rounded-2xl border border-border p-6 flex flex-col justify-between">
                <div>
                  <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                    <Server className="h-5 w-5 text-primary" /> Core Microservices
                  </h3>
                  <div className="space-y-4">
                    {stats?.systemHealth.map((sh, idx) => (
                      <div key={idx} className="p-3.5 rounded-xl border border-border bg-card/40 flex items-center justify-between">
                        <div className="flex flex-col min-w-0">
                          <span className="text-sm font-medium text-white truncate">{sh.service}</span>
                          <span className="text-xs text-gray-500 mt-1">Latency: {sh.latencyMs} ms</span>
                        </div>
                        <span className="flex h-2.5 w-2.5 relative">
                          <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${sh.status === 'UP' ? 'bg-emerald-400' : 'bg-rose-400'}`}></span>
                          <span className={`relative inline-flex rounded-full h-2.5 w-2.5 ${sh.status === 'UP' ? 'bg-emerald-500' : 'bg-rose-500'}`}></span>
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-border flex items-center justify-between text-xs text-gray-500">
                  <span>Health endpoint active at:</span>
                  <a
                    href="http://localhost:8080/actuator/health"
                    target="_blank"
                    rel="noreferrer"
                    className="text-primary hover:underline font-mono"
                  >
                    /actuator/health
                  </a>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default DashboardPage;
