/*
 * Copyright (c) 2013 LabKey Corporation
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

EXEC sp_rename 'luminex.GuideSet.TitrationName', 'ControlName', 'COLUMN';

ALTER TABLE luminex.GuideSet ADD Titration BIT
-- Need a GO here so that we can use the column in the next line
GO

UPDATE luminex.GuideSet SET Titration = 1;

ALTER TABLE luminex.GuideSet ALTER COLUMN Titration BIT NOT NULL;