-- Drop the event_speakers join table (speakers now attach at agenda level only)
DROP TABLE IF EXISTS event_speakers;

-- Drop FK constraints on organization_id columns (organizations now owned by Platform Management Service)
ALTER TABLE guest_speakers DROP CONSTRAINT IF EXISTS guest_speakers_organization_id_fkey;
ALTER TABLE events DROP CONSTRAINT IF EXISTS events_organization_id_fkey;

-- Drop the local organizations table entirely
DROP TABLE IF EXISTS organizations;