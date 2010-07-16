/*
 * Copyright (c) 2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* luminex-0.00-2.30.sql */

/* luminex-0.00-2.20.sql */

EXEC sp_addapprole 'luminex', 'password'
GO

CREATE TABLE luminex.Analyte
(
    RowId INT IDENTITY(1,1) NOT NULL,
    LSID LSIDtype NOT NULL ,
    Name VARCHAR(50) NOT NULL,
    DataId INT NOT NULL,
    FitProb REAL,
    ResVar REAL,
    RegressionType VARCHAR(100),
    StdCurve VARCHAR(255),
    MinStandardRecovery INT NOT NULL,
    MaxStandardRecovery INT NOT NULL,

    CONSTRAINT PK_Luminex_Analyte PRIMARY KEY (RowId),
    CONSTRAINT FK_LuminexAnalyte_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId)
)
GO

CREATE INDEX IX_LuminexAnalyte_DataId ON luminex.Analyte (DataId)
GO

CREATE TABLE luminex.DataRow
(
    RowId INT IDENTITY(1,1) NOT NULL,
    DataId INT NOT NULL,
    AnalyteId INT NOT NULL,
    Type VARCHAR(10),
    Well VARCHAR(50),
    Outlier BIT,
    Description VARCHAR(50),
    FIString VARCHAR(20),
    FI REAL,
    FIOORIndicator VARCHAR(10),
    FIBackgroundString VARCHAR(20),
    FIBackground REAL,
    FIBackgroundOORIndicator VARCHAR(10),
    StdDevString VARCHAR(20),
    StdDev REAL,
    StdDevOORIndicator VARCHAR(10),
    ObsConcString VARCHAR(20),
    ObsConc REAL,
    ObsConcOORIndicator VARCHAR(10),
    ExpConc REAL,
    ObsOverExp REAL,
    ConcInRangeString VARCHAR(20),
    ConcInRange REAL,
    ConcInRangeOORIndicator VARCHAR(10),

    CONSTRAINT PK_Luminex_DataRow PRIMARY KEY (RowId),
    CONSTRAINT FK_LuminexDataRow_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId),
    CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId)
)
GO

CREATE INDEX IX_LuminexDataRow_DataId ON luminex.DataRow (DataId)
GO

CREATE INDEX IX_LuminexDataRow_AnalyteId ON luminex.DataRow (AnalyteId)
GO


ALTER TABLE luminex.DataRow ADD Dilution REAL
GO

ALTER TABLE luminex.DataRow ADD DataRowGroup VARCHAR(25)
GO

ALTER TABLE luminex.DataRow ADD Ratio VARCHAR(25)
GO

ALTER TABLE luminex.DataRow ADD SamplingErrors VARCHAR(25)
GO

/* luminex-2.20-2.30.sql */

ALTER TABLE luminex.DataRow ADD PTID NVARCHAR(32)
GO

ALTER TABLE luminex.DataRow ADD VisitID FLOAT
GO

ALTER TABLE luminex.DataRow ADD Date DATETIME
GO

/* luminex-2.30-8.10.sql */

/* luminex-2.30-2.31.sql */

ALTER TABLE luminex.DataRow DROP CONSTRAINT FK_LuminexDataRow_AnalyteId
GO

ALTER TABLE luminex.DataRow ADD CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.analyte(RowId)
GO

/* luminex-2.31-2.32.sql */

ALTER TABLE luminex.DataRow ADD ExtraSpecimenInfo NVARCHAR(50)
GO

/* luminex-8.10-8.20.sql */

/* luminex-8.10-8.11.sql */

UPDATE luminex.Analyte SET fitprob = NULL, resvar = NULL WHERE fitprob = 0 AND resvar = 0
GO