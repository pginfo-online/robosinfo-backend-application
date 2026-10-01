-- =====================================================================
-- V6__seed_super_admin_user.sql
-- Seed Super Administrator account and assign ROLE_ADMIN and ROLE_SUPER_ADMIN
-- Credentials:
--   Email:    admin@shopmart.in
--   Password: Ms24GyFCadRfUS4FcJ1XESXgScQA19zC
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_admin_id UUID;
    v_password_hash VARCHAR(255);
BEGIN
    -- Generate standard BCrypt hash (cost 12) for 'Ms24GyFCadRfUS4FcJ1XESXgScQA19zC'
    v_password_hash := crypt('Ms24GyFCadRfUS4FcJ1XESXgScQA19zC', gen_salt('bf', 12));

    -- Upsert admin in identity_schema.users
    INSERT INTO identity_schema.users (
        phone,
        email,
        name,
        user_type,
        status,
        password_hash,
        created_at,
        updated_at
    ) VALUES (
        '0000000000',
        'admin@shopmart.in',
        'Super Administrator',
        'ADMIN',
        'ACTIVE',
        v_password_hash,
        NOW(),
        NOW()
    )
    ON CONFLICT (email) DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        status = 'ACTIVE',
        user_type = 'ADMIN',
        name = COALESCE(identity_schema.users.name, EXCLUDED.name),
        updated_at = NOW()
    RETURNING id INTO v_admin_id;

    IF v_admin_id IS NULL THEN
        SELECT id INTO v_admin_id FROM identity_schema.users WHERE email = 'admin@shopmart.in';
    END IF;

    -- Grant ROLE_ADMIN
    INSERT INTO identity_schema.user_roles (user_id, role, granted_at)
    VALUES (v_admin_id, 'ROLE_ADMIN', NOW())
    ON CONFLICT (user_id, role, resource_id) DO NOTHING;

    -- Grant ROLE_SUPER_ADMIN
    INSERT INTO identity_schema.user_roles (user_id, role, granted_at)
    VALUES (v_admin_id, 'ROLE_SUPER_ADMIN', NOW())
    ON CONFLICT (user_id, role, resource_id) DO NOTHING;

    RAISE NOTICE 'Admin user seeded successfully with ID: %', v_admin_id;
END $$;
