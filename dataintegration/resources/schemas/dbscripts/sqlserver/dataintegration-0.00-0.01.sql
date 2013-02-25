CREATE SCHEMA dataintegration
GO

CREATE PROCEDURE dataintegration.addDataIntegrationColumns @schemaName NVARCHAR(100), @tableName NVARCHAR(100)
AS
DECLARE @sql NVARCHAR(1000)
SELECT @sql = 'ALTER TABLE [' + @schemaName + '].[' + @tableName + '] ADD  ' +
     '_txRowVersion ROWVERSION, ' +
     '_txLastUpdated DATETIME, ' +
     '_txTransactionId INT, ' +
     '_txNote NVARCHAR(1000)';
EXEC sp_executesql @sql;
SELECT @sql = 'ALTER TABLE [' + @schemaName + '].[' + @tableName + '] ADD CONSTRAINT [_DF_' + @tableName + '_updated] ' +
    'DEFAULT getutcdate() FOR [_txLastUpdated]';
EXEC sp_executesql @sql;

GO

CREATE TABLE dataintegration.TransformRun
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    RecordCount INT,
    JobId INT NOT NULL,
    TransformId NVARCHAR(50) NOT NULL,
    TransformVersion INT NOT NULL,
    Status NVARCHAR(500),
    StartTime DATETIME NULL,
    EndTime DATETIME NULL,
    Created DATETIME NULL,
    CreatedBy INT NULL,
    Modified DATETIME NULL,
    ModifiedBy INT NULL,

    CONSTRAINT FK_TransformRun_JobId FOREIGN KEY (JobId) REFERENCES pipeline.StatusFiles (RowId),
    CONSTRAINT FK_TransformRun_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

CREATE INDEX IDX_TransformRun_JobId ON dataintegration.TransformRun(JobId);
CREATE INDEX IDX_TransformRun_Container ON dataintegration.TransformRun(Container);

CREATE TABLE dataintegration.TransformConfiguration
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    Container ENTITYID NOT NULL,
    TransformId VARCHAR(50) NOT NULL,
    Enabled BIT,
    VerboseLogging BIT,
    LastChecked DATETIME NULL,
    Created DATETIME NULL,
    CreatedBy INT NULL,
    Modified DATETIME NULL,
    ModifiedBy INT NULL,

    CONSTRAINT UQ_TransformConfiguration_TransformId UNIQUE (TransformId),
    CONSTRAINT FK_TransformConfiguration_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

CREATE INDEX IDX_TransformConfiguration_Container ON dataintegration.TransformRun(Container);
