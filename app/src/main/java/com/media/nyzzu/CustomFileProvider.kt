package com.media.nyzzu

import androidx.core.content.FileProvider

// needed, because creative editor library uses FileProvider,
// but we also need one for saving temporary files in the cache
class CustomFileProvider : FileProvider()
