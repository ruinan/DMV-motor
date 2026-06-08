-- Stripe billing wiring (per-exam monthly subscriptions via hosted Checkout).
--
--  exams.stripe_price_id          the Stripe Price ($5/mo) a checkout subscribes to.
--                                 NULL = not purchasable yet (Subscribe is disabled).
--  users.stripe_customer_id       the user's Stripe Customer, created on first checkout.
--  access_passes.stripe_subscription_id
--                                 links a pass to the Stripe Subscription that funds it,
--                                 so subscription webhooks can extend / cancel the right pass.
ALTER TABLE exams         ADD COLUMN IF NOT EXISTS stripe_price_id        TEXT;
ALTER TABLE users         ADD COLUMN IF NOT EXISTS stripe_customer_id     TEXT;
ALTER TABLE access_passes ADD COLUMN IF NOT EXISTS stripe_subscription_id TEXT;

CREATE INDEX IF NOT EXISTS idx_access_passes_stripe_sub
    ON access_passes (stripe_subscription_id);
