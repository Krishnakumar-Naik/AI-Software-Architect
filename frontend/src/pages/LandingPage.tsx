import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Shield, Cpu, BookOpen, BarChart3, ArrowRight, Activity } from 'lucide-react';

const LandingPage: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background bg-grid text-foreground relative overflow-hidden flex flex-col">
      {/* Background Glow */}
      <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] rounded-full bg-primary/10 blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] rounded-full bg-indigo-500/10 blur-[120px] pointer-events-none"></div>

      {/* Navigation Header */}
      <header className="glass-panel w-full sticky top-0 z-50 border-b border-border">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-lg bg-gradient-to-br from-primary to-indigo-600 flex items-center justify-center shadow-lg shadow-primary/20">
              <Cpu className="h-5 w-5 text-white" />
            </div>
            <span className="font-semibold text-lg tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-white to-gray-300">
              AI Software Architect
            </span>
          </div>

          <div className="flex items-center gap-4">
            {isAuthenticated ? (
              <button
                onClick={() => navigate('/dashboard')}
                className="px-4 py-2 rounded-lg bg-primary hover:bg-primary/95 text-white font-medium transition-all duration-200 shadow-lg shadow-primary/20 hover:scale-[1.02]"
              >
                Go to Dashboard
              </button>
            ) : (
              <>
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm text-gray-300 hover:text-white font-medium transition-all"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-2 rounded-lg bg-primary hover:bg-primary/95 text-white font-medium text-sm transition-all duration-200 shadow-lg shadow-primary/25 hover:scale-[1.02]"
                >
                  Get Started
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="flex-1 flex flex-col items-center justify-center max-w-7xl mx-auto px-6 py-20 relative z-10 text-center">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-primary/25 bg-primary/5 text-primary text-xs font-semibold tracking-wide uppercase mb-6 animate-pulse">
          <Activity className="h-3.5 w-3.5" /> Platform Foundation Live
        </div>
        
        <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight max-w-4xl leading-[1.15] mb-6">
          Analyze Codebase Architecture with{' '}
          <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary via-blue-400 to-indigo-500">
            AI Precision
          </span>
        </h1>
        
        <p className="text-gray-400 text-lg md:text-xl max-w-2xl mb-10 leading-relaxed">
          Map dependencies, identify design anomalies, write high-quality technical documentation, and receive expert architectural guidance in seconds.
        </p>

        <div className="flex flex-col sm:flex-row gap-4 mb-20">
          <button
            onClick={() => navigate(isAuthenticated ? '/dashboard' : '/register')}
            className="px-8 py-4 rounded-xl bg-primary hover:bg-primary/95 text-white font-semibold flex items-center gap-2 transition-all duration-200 shadow-lg shadow-primary/30 hover:scale-[1.02]"
          >
            Start Mapping Free <ArrowRight className="h-5 w-5" />
          </button>
          <button
            onClick={() => navigate('/demo')}
            className="px-8 py-4 rounded-xl border border-border bg-card/45 hover:bg-card/90 text-gray-200 hover:text-white font-semibold transition-all duration-200"
          >
            Explore Demo
          </button>
        </div>

        {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 w-full max-w-6xl">
          <div className="glass-card p-6 rounded-2xl flex flex-col items-start text-left transition-all duration-200 hover:border-primary/30 hover:translate-y-[-4px]">
            <div className="h-12 w-12 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary mb-4 shadow-sm">
              <Cpu className="h-6 w-6" />
            </div>
            <h3 className="font-semibold text-lg mb-2">Projects Analyzed</h3>
            <p className="text-sm text-gray-400">Map directory structures and trace dependency chains automatically.</p>
          </div>

          <div className="glass-card p-6 rounded-2xl flex flex-col items-start text-left transition-all duration-200 hover:border-indigo-500/30 hover:translate-y-[-4px]">
            <div className="h-12 w-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-4 shadow-sm">
              <BarChart3 className="h-6 w-6" />
            </div>
            <h3 className="font-semibold text-lg mb-2">Architecture Reports</h3>
            <p className="text-sm text-gray-400">Get granular visual analysis of packages, boundaries, and class relationships.</p>
          </div>

          <div className="glass-card p-6 rounded-2xl flex flex-col items-start text-left transition-all duration-200 hover:border-emerald-500/30 hover:translate-y-[-4px]">
            <div className="h-12 w-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-4 shadow-sm">
              <BookOpen className="h-6 w-6" />
            </div>
            <h3 className="font-semibold text-lg mb-2">Documentation Generated</h3>
            <p className="text-sm text-gray-400">Instantly generate high-quality diagrams, design docs, and Markdown READMEs.</p>
          </div>

          <div className="glass-card p-6 rounded-2xl flex flex-col items-start text-left transition-all duration-200 hover:border-rose-500/30 hover:translate-y-[-4px]">
            <div className="h-12 w-12 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400 mb-4 shadow-sm">
              <Shield className="h-6 w-6" />
            </div>
            <h3 className="font-semibold text-lg mb-2">AI Conversations</h3>
            <p className="text-sm text-gray-400">Converse directly with Gemini models to refactor patterns and resolve structural debt.</p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="glass-panel w-full border-t border-border py-8 mt-auto relative z-10 text-center text-sm text-gray-500">
        <p>© 2026 AI Software Architect. All rights reserved. Built with React & Spring Boot.</p>
      </footer>
    </div>
  );
};

export default LandingPage;
