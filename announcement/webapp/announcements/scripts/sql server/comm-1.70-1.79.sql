ALTER TABLE comm.Announcements ADD DiscussionSrcIdentifier NVARCHAR(100) NULL
ALTER TABLE comm.Announcements ADD DiscussionSrcURL NVARCHAR(1000) NULL
GO

CREATE INDEX IX_DiscussionSrcIdentifier ON comm.announcements(Container, DiscussionSrcIdentifier)
GO

-- Add DiscussionSrcIdentifier, DiscussionSrcURL to threads view
DROP VIEW comm.Threads
GO

CREATE VIEW comm.Threads AS
    SELECT y.RowId, y.EntityId, y.Container, y.Body, y.RendererType, PropsId AS LatestId, props.Title, props.AssignedTo, props.Status, props.Expires, props.CreatedBy AS ResponseCreatedBy, props.Created AS ResponseCreated, y.DiscussionSrcIdentifier, y.DiscussionSrcURL, y.CreatedBy, y.Created FROM
    (
        SELECT *, CASE WHEN LastResponseId IS NULL THEN x.RowId ELSE LastResponseId END AS PropsId FROM
        (
            SELECT *, (SELECT MAX(RowId) FROM comm.Announcements response WHERE response.Parent = message.EntityId) AS LastResponseId
            FROM comm.Announcements message
            WHERE Parent IS NULL
        ) x
    ) y LEFT OUTER JOIN comm.Announcements props ON props.RowId = PropsId
GO

-- Better descriptions for existing email options
UPDATE comm.EmailOptions SET EmailOption = 'No email' WHERE EmailOptionId = 0
UPDATE comm.EmailOptions SET EmailOption = 'All conversations' WHERE EmailOptionId = 1
UPDATE comm.EmailOptions SET EmailOption = 'My conversations' WHERE EmailOptionId = 2
GO

-- Add new daily digest options
INSERT INTO comm.EmailOptions (EmailOptionId, EmailOption) VALUES (257, 'Daily digest of all conversations')
INSERT INTO comm.EmailOptions (EmailOptionId, EmailOption) VALUES (258, 'Daily digest of my conversations')
GO

-- Change folder defaults from 'None' to 'My conversations' (email is sent if you're on the member list or if you've posted to a conversation)
UPDATE prop.Properties SET Value = '2' WHERE
    "Set" IN (SELECT "Set" FROM prop.PropertySets WHERE Category = 'defaultEmailSettings' AND UserId = 0) AND
    Name = 'defaultEmailOption' AND
    Value = '0'
GO
