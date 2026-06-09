-- Add public sharing capability to projects
ALTER TABLE projects ADD COLUMN is_public BOOLEAN DEFAULT FALSE;
