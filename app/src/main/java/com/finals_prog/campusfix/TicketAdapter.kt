package com.finals_prog.campusfix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.google.android.material.button.MaterialButton

/** Pass null callbacks (resolved list) to hide the matching buttons. */
class TicketAdapter(
    private val onAcknowledgeClick: ((Report) -> Unit)?,
    private val onUpdateClick: ((Report) -> Unit)?,
    private val onItemClick: ((Report) -> Unit)? = null
) : ListAdapter<Report, TicketAdapter.VH>(DIFF) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTicketTitle)
        val status: TextView = view.findViewById(R.id.tvTicketStatus)
        val btnAcknowledge: MaterialButton = view.findViewById(R.id.btnAcknowledgedStatus)
        val btnUpdate: MaterialButton = view.findViewById(R.id.btnUpdateStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_ticket, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val report = getItem(position)
        holder.title.text = "[${report.category}] ${report.building} - ${report.room}"
        holder.status.text = "${report.title} | ${report.status}"

        // Step 1: Reported -> "Start working" (Acknowledge)
        val canAcknowledge = onAcknowledgeClick != null &&
                report.status.equals(Report.STATUS_REPORTED, ignoreCase = true)
        holder.btnAcknowledge.isVisible = canAcknowledge
        holder.btnAcknowledge.setOnClickListener { onAcknowledgeClick?.invoke(report) }

        // Step 2: Acknowledged -> "Update Status" (Resolve)
        val canResolve = onUpdateClick != null &&
                report.status.equals(Report.STATUS_ACKNOWLEDGED, ignoreCase = true)
        holder.btnUpdate.isVisible = canResolve
        holder.btnUpdate.setOnClickListener { onUpdateClick?.invoke(report) }
        if (onItemClick != null) {
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