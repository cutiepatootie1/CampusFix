package com.finals_prog.campusfix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.Users
import com.finals_prog.campusfix.utils.ImageLoader
import com.google.android.material.checkbox.MaterialCheckBox

/** Ticket rows for the admin lists (item_admin_ticket.xml). */
class AdminTicketAdapter(
    private val onClick: (Report) -> Unit
) : ListAdapter<Report, AdminTicketAdapter.VH>(DIFF) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.ivTicketThumb)
        val id: TextView = view.findViewById(R.id.tvTicketId)
        val title: TextView = view.findViewById(R.id.tvTicketTitle)
        val location: TextView = view.findViewById(R.id.tvBuildingRoom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_admin_ticket, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = getItem(position)
        holder.id.text = "${r.status} • #${r.reportId.take(8)}"
        holder.title.text = r.title
        holder.location.text = "${r.building} | ${r.room}"
        ImageLoader.load(holder.thumb, r.photoUrl, 200)
        holder.itemView.setOnClickListener { onClick(r) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Report>() {
            override fun areItemsTheSame(a: Report, b: Report) = a.reportId == b.reportId
            override fun areContentsTheSame(a: Report, b: Report) = a == b
        }
    }
}

/** Checklist rows inside the "Assign Maintenance Staff" dialog (item_staff_checklist.xml). */
class StaffChecklistAdapter(
    private val staff: List<Users>,
    val selected: MutableSet<String>
) : RecyclerView.Adapter<StaffChecklistAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.ivStaffAvatar)
        val name: TextView = view.findViewById(R.id.tvStaffName)
        val email: TextView = view.findViewById(R.id.tvStaffEmail)
        val check: MaterialCheckBox = view.findViewById(R.id.cbStaff)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_staff_checklist, parent, false)
    )

    override fun getItemCount() = staff.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = staff[position]
        holder.name.text = user.fullName.ifBlank { user.email }
        holder.email.text = user.email
        bindAvatar(holder.avatar, user)

        holder.check.setOnCheckedChangeListener(null) // avoid firing while rebinding
        holder.check.isChecked = user.uid in selected
        holder.check.setOnCheckedChangeListener { _, checked ->
            if (checked) selected.add(user.uid) else selected.remove(user.uid)
        }
        holder.itemView.setOnClickListener { holder.check.toggle() }
    }
}

/** Staff already on a ticket (item_staff_assigned.xml). */
class AssignedStaffAdapter : ListAdapter<Users, AssignedStaffAdapter.VH>(DIFF) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.ivAssignedAvatar)
        val name: TextView = view.findViewById(R.id.tvAssignedName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_staff_assigned, parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = getItem(position)
        holder.name.text = user.fullName.ifBlank { user.email.ifBlank { "Unknown staff" } }
        bindAvatar(holder.avatar, user)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Users>() {
            override fun areItemsTheSame(a: Users, b: Users) = a.uid == b.uid
            override fun areContentsTheSame(a: Users, b: Users) = a == b
        }
    }
}

private fun bindAvatar(view: ImageView, user: Users) {
    when {
        !user.avatarUrl.isNullOrBlank() -> ImageLoader.load(view, user.avatarUrl, 120)
        user.avatarResId != 0 -> {
            view.tag = null
            view.setImageResource(user.avatarResId)
        }
        else -> {
            view.tag = null
            view.setImageDrawable(null)
        }
    }
}