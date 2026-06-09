CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    repository_url VARCHAR(255),
    repo_owner VARCHAR(100),
    repository_name VARCHAR(100),
    branch_name VARCHAR(50),
    owner_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    version INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    total_files INT DEFAULT 0,
    total_classes INT DEFAULT 0,
    total_interfaces INT DEFAULT 0,
    total_services INT DEFAULT 0,
    total_controllers INT DEFAULT 0,
    total_endpoints INT DEFAULT 0,
    analysis_json LONGTEXT,
    architecture_summary LONGTEXT,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE source_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_id BIGINT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_name VARCHAR(100) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    content LONGTEXT,
    FOREIGN KEY (analysis_id) REFERENCES analyses(id) ON DELETE CASCADE
);

CREATE TABLE class_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_file_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    stereotype VARCHAR(20) NOT NULL,
    package_name VARCHAR(255),
    FOREIGN KEY (source_file_id) REFERENCES source_files(id) ON DELETE CASCADE
);

CREATE TABLE method_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_metadata_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    return_type VARCHAR(100),
    is_api_endpoint BOOLEAN DEFAULT FALSE,
    http_method VARCHAR(10),
    path VARCHAR(255),
    FOREIGN KEY (class_metadata_id) REFERENCES class_metadata(id) ON DELETE CASCADE
);

CREATE TABLE dependency_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_id BIGINT NOT NULL,
    source_node VARCHAR(255) NOT NULL,
    target_node VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    FOREIGN KEY (analysis_id) REFERENCES analyses(id) ON DELETE CASCADE
);
