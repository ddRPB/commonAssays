/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

-- DROP all views (current and obsolete)

-- NOTE: Don't remove any of these drop statements, even if we stop re-creating the view in *-create.sql. Drop statements must
-- remain in place so we can correctly upgrade from older versions, which we commit to for two years after each release.

SELECT core.fn_dropifexists('Peptides', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('SimplePeptides', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('ProteinGroupsWithQuantitation', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('ExperimentRuns', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('FastaAdmin', 'prot', 'VIEW', NULL);
SELECT core.fn_dropifexists('Spectra', 'ms2', 'VIEW', NULL);
