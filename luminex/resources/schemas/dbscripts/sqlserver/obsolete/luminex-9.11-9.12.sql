/*
 * Copyright (c) 2009-2010 LabKey Corporation
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

-- Add denormalized data to improve query performance
ALTER TABLE luminex.datarow ADD Container UniqueIdentifier
GO
ALTER TABLE luminex.datarow ADD ProtocolID INT
GO

-- Set the values for the new columns in existing rows
UPDATE luminex.datarow SET Container = (SELECT container FROM exp.data d WHERE d.rowid = dataid)
GO
UPDATE luminex.datarow SET ProtocolID =
  (SELECT p.RowId FROM exp.experimentrun r, exp.data d, exp.protocol p 
    WHERE d.rowid = dataid AND r.rowid = d.runid AND r.protocollsid = p.lsid)
GO

-- Lock down the values in the columns
ALTER TABLE luminex.datarow ALTER COLUMN Container EntityID NOT NULL
GO
ALTER TABLE luminex.datarow ALTER COLUMN ProtocolID INT NOT NULL
GO

ALTER TABLE luminex.datarow
	ADD CONSTRAINT FK_DataRow_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityID)
GO

ALTER TABLE luminex.datarow
	ADD CONSTRAINT FK_DataRow_ProtocolID FOREIGN KEY (ProtocolID) REFERENCES exp.Protocol (RowID)
GO

CREATE INDEX IDX_DataRow_Container_ProtocolID ON luminex.datarow(Container, ProtocolID)
GO
