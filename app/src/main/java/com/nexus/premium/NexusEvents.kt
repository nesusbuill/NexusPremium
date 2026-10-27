package com.nexus.premium

object NexusEvents {
    var onStatus: ((String) -> Unit)? = null
    var onResult: ((String) -> Unit)? = null
}
