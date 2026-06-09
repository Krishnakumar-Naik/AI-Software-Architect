import React from 'react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, User, LogOut, Cpu, ChevronLeft, ChevronRight, Upload, Layers } from 'lucide-react';

interface SidebarProps {
  collapsed: boolean;
  setCollapsed: (c: boolean) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ collapsed, setCollapsed }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { projectId } = useParams<{ projectId?: string }>();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navItems = [
    {
      name: 'Dashboard',
      path: '/dashboard',
      icon: <LayoutDashboard className="h-5 w-5" />,
    },
    {
      name: 'Upload Project',
      path: '/upload',
      icon: <Upload className="h-5 w-5" />,
    },
  ];

  if (projectId) {
    navItems.push(
      {
        name: 'Architecture Graph',
        path: `/projects/${projectId}/architecture`,
        icon: <Layers className="h-5 w-5" />,
      },
      {
        name: 'AI Assistant',
        path: `/projects/${projectId}/assistant`,
        icon: <Cpu className="h-5 w-5" />,
      }
    );
  }

  navItems.push({
    name: 'Profile Settings',
    path: '/profile',
    icon: <User className="h-5 w-5" />,
  });

  return (
    <aside
      className={`glass-panel h-screen border-r border-border flex flex-col justify-between transition-all duration-300 z-30 shrink-0 ${
        collapsed ? 'w-20' : 'w-64'
      }`}
    >
      <div>
        {/* Brand Header */}
        <div className="h-16 border-b border-border flex items-center justify-between px-4">
          <div className="flex items-center gap-3 overflow-hidden">
            <div className="h-9 w-9 shrink-0 rounded-lg bg-gradient-to-br from-primary to-indigo-600 flex items-center justify-center shadow-lg shadow-primary/20">
              <Cpu className="h-5 w-5 text-white" />
            </div>
            {!collapsed && (
              <span className="font-bold text-sm tracking-tight text-white whitespace-nowrap">
                AI Architect
              </span>
            )}
          </div>
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="p-1 rounded bg-card hover:bg-secondary border border-border text-gray-400 hover:text-white"
          >
            {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </button>
        </div>

        {/* Navigation Items */}
        <nav className="p-4 space-y-2">
          {navItems.map((item) => (
            <NavLink
              key={item.name}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center gap-4 px-4 py-3 rounded-xl transition-all duration-200 group ${
                  isActive
                    ? 'bg-primary text-white shadow-md shadow-primary/25 font-medium'
                    : 'text-gray-400 hover:text-white hover:bg-secondary/40'
                }`
              }
            >
              <div className="shrink-0">{item.icon}</div>
              {!collapsed && <span className="text-sm">{item.name}</span>}
            </NavLink>
          ))}
        </nav>
      </div>

      {/* User Actions Footer */}
      <div className="p-4 border-t border-border space-y-4">
        {/* User Card */}
        <div className="flex items-center gap-3 overflow-hidden">
          <div className="h-10 w-10 shrink-0 rounded-full bg-gradient-to-tr from-indigo-500 to-primary flex items-center justify-center font-bold text-white shadow-inner">
            {user?.username?.substring(0, 2).toUpperCase() || 'US'}
          </div>
          {!collapsed && (
            <div className="flex flex-col min-w-0">
              <span className="text-sm font-medium text-white truncate">{user?.username}</span>
              <span className="text-xs text-gray-500 truncate">{user?.email}</span>
            </div>
          )}
        </div>

        {/* Logout Button */}
        <button
          onClick={handleLogout}
          className="flex items-center gap-4 w-full px-4 py-3 rounded-xl transition duration-200 text-rose-400 hover:text-rose-300 hover:bg-rose-500/10"
        >
          <LogOut className="h-5 w-5 shrink-0" />
          {!collapsed && <span className="text-sm font-medium">Sign Out</span>}
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
