package com.finals_prog.campusfix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.utils.ImageLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for report/ticket cards in CampusFix.
 *
 * - [onStartClick]   (maintenance staff) "Start Work" button, shown while a ticket is Acknowledged.
 * - [onUpdateClick]  (maintenance staff) "Mark Resolved" button, shown while a ticket is In Progress.
 * - [onItemClick]    tap on the whole card.
 * Pass null for any of them to hide that action (e.g. the reporter's dashboard passes none).
 */
class TicketAdapter(
    private val onStartClick: ((Report) -> Unit)? = null,
    private val onUpdateClick: ((Report) -> Unit)? = null,
    private val onItemClick: ((Report) -> Unit)? = null
) : ListAdapter<Report, TicketAdapter.VH>(DIFF) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cardThumbnail: MaterialCardView = view.findViewById(R.id.cardThumbnail)
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val ticketId: TextView = view.findViewById(R.id.tvTicketId)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val location: TextView = view.findViewById(R.id.tvLocation)
        val actions: View = view.findViewById(R.id.llActions)
        val btnStart: MaterialButton = view.findViewById(R.id.btnStartWork)
        val btnResolve: MaterialButton = view.findViewById(R.id.btnResolve)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_ticket, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val report = getItem(position)
        val context = holder.itemView.context

        holder.ticketId.text = if (report.reportId.isNotBlank()) {
            "#${report.reportId.take(8)}"
        } else {
            "#TICKET"
        }

        holder.title.text = report.title.ifBlank {
            "[${report.category}] ${report.building} - ${report.room}".trim()
        }

        val locationParts = listOfNotNull(
            report.building.takeIf { it.isNotBlank() },
            report.room.takeIf { it.isNotBlank() }
        )
        val locationText = if (locationParts.isNotEmpty()) {
            locationParts.joinToString(" - ")
        } else {
            "Location not specified"
        }
        holder.location.text = if (report.category.isNotBlank()) {
            "$locationText • ${report.category}"
        } else {
            locationText
        }

        holder.status.text = when {
            report.isResolved -> "RESOLVED"
            report.status.isNotBlank() -> report.status.uppercase()
            else -> "ACTIVE"
        }

        val statusColorRes = when {
            report.isResolved -> R.color.campus_secondary
            report.isInProgress -> R.color.campus_primary
            else -> R.color.campus_accent_amber
        }
        holder.status.setTextColor(ContextCompat.getColor(context, statusColorRes))

        // Thumbnail: photoUrl is a Cloudinary https URL, loaded (and cached) by ImageLoader.
        if (report.photoUrl.isNotBlank()) {
            holder.cardThumbnail.isVisible = true
            ImageLoader.load(holder.ivPhoto, report.photoUrl, 200)
        } else {
            holder.cardThumbnail.isVisible = false
            ImageLoader.load(holder.ivPhoto, null)
        }

        // Staff actions: one button at a time, following the ticket's status.
        val canStart = onStartClick != null && !report.isInProgress && !report.isResolved
        val canResolve = onUpdateClick != null && report.isInProgress
        holder.btnStart.isVisible = canStart
        holder.btnResolve.isVisible = canResolve
        holder.actions.isVisible = canStart || canResolve
        holder.btnStart.setOnClickListener { onStartClick?.invoke(report) }
        holder.btnResolve.setOnClickListener { onUpdateClick?.invoke(report) }

        if (onItemClick != null) {
            holder.itemView.isClickable = true
            holder.itemView.setOnClickListener { onItemClick.invoke(report) }
        } else {
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Report>() {
            override fun areItemsTheSame(a: Report, b: Report) = a.reportId == b.reportId
            override fun areContentsTheSame(a: Report, b: Report) = a == b
        }
    }
}