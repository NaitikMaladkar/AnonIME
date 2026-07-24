package com.anonime

import android.app.Application

/**
 * Application entry point.
 *
 * Phase 1 has no global state to initialize — included so future phases can
 * hook a settings store, theme cache, or migration logic here without
 * touching the manifest again.
 */
class AnonIMEApplication : Application()
