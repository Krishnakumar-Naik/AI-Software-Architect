import React, { useState } from 'react';
import Sidebar from '../components/Sidebar';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { User, Lock, Save, Key, CheckCircle, AlertCircle } from 'lucide-react';

const ProfilePage: React.FC = () => {
  const { user, updateUser } = useAuth();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // Profile Form State
  const [username, setUsername] = useState(user?.username || '');
  const [email, setEmail] = useState(user?.email || '');
  const [profileSuccess, setProfileSuccess] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [profileSubmitting, setProfileSubmitting] = useState(false);

  // Password Form State
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !email.trim()) {
      setProfileError('Username and email are required');
      return;
    }

    try {
      setProfileError(null);
      setProfileSuccess(null);
      setProfileSubmitting(true);

      const response = await api.put('/api/users/profile', { username, email });
      // Update state in context
      updateUser(response.data.username, response.data.email);
      setProfileSuccess('Profile details updated successfully');
    } catch (err: any) {
      console.error(err);
      if (err.response?.data?.message) {
        setProfileError(err.response.data.message);
      } else {
        setProfileError('Failed to update profile details.');
      }
    } finally {
      setProfileSubmitting(false);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!oldPassword || !newPassword || !confirmPassword) {
      setPasswordError('All password fields are required');
      return;
    }

    if (newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError('New password and confirm password do not match');
      return;
    }

    try {
      setPasswordError(null);
      setPasswordSuccess(null);
      setPasswordSubmitting(true);

      await api.put('/api/users/password', { oldPassword, newPassword });
      setPasswordSuccess('Password updated successfully');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      console.error(err);
      if (err.response?.data?.message) {
        setPasswordError(err.response.data.message);
      } else {
        setPasswordError('Failed to update password. Make sure current password is correct.');
      }
    } finally {
      setPasswordSubmitting(false);
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
          <h1 className="text-3xl font-bold tracking-tight text-white">Account Workspace</h1>
          <p className="text-gray-400 text-sm mt-1">Manage credentials, passwords, and details</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 relative z-10 max-w-5xl">
          {/* Edit Profile Form */}
          <div className="glass-card p-6 rounded-2xl border border-border flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-3 border-b border-border pb-4 mb-6">
                <div className="p-2 rounded-lg bg-primary/10 text-primary border border-primary/20">
                  <User className="h-5 w-5" />
                </div>
                <h3 className="text-lg font-bold text-white">Profile Details</h3>
              </div>

              {profileSuccess && (
                <div className="mb-5 p-4 rounded-lg bg-emerald-500/10 border border-emerald-500/25 text-emerald-400 text-sm flex items-start gap-3">
                  <CheckCircle className="h-5 w-5 shrink-0 mt-0.5" />
                  <span>{profileSuccess}</span>
                </div>
              )}

              {profileError && (
                <div className="mb-5 p-4 rounded-lg bg-destructive/10 border border-destructive/25 text-destructive-foreground text-sm flex items-start gap-3">
                  <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
                  <span>{profileError}</span>
                </div>
              )}

              <form onSubmit={handleProfileSubmit} className="space-y-5">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                    Username
                  </label>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white placeholder-gray-500 focus:outline-none focus:border-primary/50 focus:ring-1 focus:ring-primary/50 transition"
                    disabled={profileSubmitting}
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                    Email Address
                  </label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white placeholder-gray-500 focus:outline-none focus:border-primary/50 focus:ring-1 focus:ring-primary/50 transition"
                    disabled={profileSubmitting}
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={profileSubmitting}
                  className="w-full py-3 rounded-xl bg-primary hover:bg-primary/95 text-white font-semibold flex items-center justify-center gap-2 transition duration-200 shadow-lg shadow-primary/25 disabled:opacity-50"
                >
                  {profileSubmitting ? (
                    <>
                      <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
                      Updating...
                    </>
                  ) : (
                    <>
                      <Save className="h-5 w-5" />
                      Save Changes
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>

          {/* Change Password Form */}
          <div className="glass-card p-6 rounded-2xl border border-border flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-3 border-b border-border pb-4 mb-6">
                <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                  <Key className="h-5 w-5" />
                </div>
                <h3 className="text-lg font-bold text-white">Security Settings</h3>
              </div>

              {passwordSuccess && (
                <div className="mb-5 p-4 rounded-lg bg-emerald-500/10 border border-emerald-500/25 text-emerald-400 text-sm flex items-start gap-3">
                  <CheckCircle className="h-5 w-5 shrink-0 mt-0.5" />
                  <span>{passwordSuccess}</span>
                </div>
              )}

              {passwordError && (
                <div className="mb-5 p-4 rounded-lg bg-destructive/10 border border-destructive/25 text-destructive-foreground text-sm flex items-start gap-3">
                  <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
                  <span>{passwordError}</span>
                </div>
              )}

              <form onSubmit={handlePasswordSubmit} className="space-y-5">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                    Current Password
                  </label>
                  <input
                    type="password"
                    placeholder="Enter current password"
                    value={oldPassword}
                    onChange={(e) => setOldPassword(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500/50 focus:ring-1 focus:ring-indigo-500/50 transition"
                    disabled={passwordSubmitting}
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                    New Password
                  </label>
                  <input
                    type="password"
                    placeholder="Enter new password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500/50 focus:ring-1 focus:ring-indigo-500/50 transition"
                    disabled={passwordSubmitting}
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
                    Confirm New Password
                  </label>
                  <input
                    type="password"
                    placeholder="Confirm new password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-border bg-card/65 text-white placeholder-gray-500 focus:outline-none focus:border-indigo-500/50 focus:ring-1 focus:ring-indigo-500/50 transition"
                    disabled={passwordSubmitting}
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={passwordSubmitting}
                  className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold flex items-center justify-center gap-2 transition duration-200 shadow-lg shadow-indigo-500/25 disabled:opacity-50"
                >
                  {passwordSubmitting ? (
                    <>
                      <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
                      Updating Password...
                    </>
                  ) : (
                    <>
                      <Lock className="h-5 w-5" />
                      Update Password
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default ProfilePage;
