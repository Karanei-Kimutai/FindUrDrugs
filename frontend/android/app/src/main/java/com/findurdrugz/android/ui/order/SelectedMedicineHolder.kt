package com.findurdrugz.android.ui.order

import com.findurdrugz.android.data.model.MedicineResult

/**
 * Simple MVP-level holder to pass the selected search result into OrderScreen
 * without complex nav argument serialization (Navigation Compose doesn't easily
 * pass full objects between screens — only primitives/strings by default).
 *
 * This is a pragmatic shortcut, not ideal architecture: it's global mutable state,
 * so it could be overwritten unexpectedly if misused (e.g. two screens both writing
 * to it at once). Fine for a single-activity MVP flow (select → order, always in
 * that order). Revisit with a shared ViewModel or SavedStateHandle if the app
 * grows more screens that need to pass complex data around.
 */
object SelectedMedicineHolder {
    var selected: MedicineResult? = null
}