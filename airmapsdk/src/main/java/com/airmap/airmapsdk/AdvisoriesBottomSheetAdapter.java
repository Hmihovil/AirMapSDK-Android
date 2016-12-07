package com.airmap.airmapsdk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.airmap.airmapsdk.models.status.AirMapStatus;
import com.airmap.airmapsdk.models.status.AirMapStatusAdvisory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Vansh Gandhi on 11/8/16.
 * Copyright © 2016 AirMap, Inc. All rights reserved.
 */

public class AdvisoriesBottomSheetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_TFR = 2;
    private static final int TYPE_WILDFIRE = 3;

    private static final String HEADER_STRING = "header";

    private final String RED_TITLE;
    private final String YELLOW_TITLE;
    private final String GREEN_TITLE;

    private List<AirMapStatusAdvisory> advisories = new ArrayList<>();
    private Map<String,String> organizationsMap;
    private Context context;


    public AdvisoriesBottomSheetAdapter(Context context, Map<String, List<AirMapStatusAdvisory>> data, Map<String, String> organizations) {
        this.context = context;
        RED_TITLE = context.getString(R.string.flight_strictly_regulated);
        YELLOW_TITLE = context.getString(R.string.advisories);
        GREEN_TITLE = context.getString(R.string.informational);

        if (data.containsKey(RED_TITLE)) {
            AirMapStatusAdvisory header = new AirMapStatusAdvisory();
            header.setName(RED_TITLE);
            header.setId(HEADER_STRING); //Hack to have only a list of Advisories, but let it know it's a header
            advisories.add(header);
            for (AirMapStatusAdvisory advisory : data.get(RED_TITLE)) {
                advisories.add(advisory);
            }
        }

        if (data.containsKey(YELLOW_TITLE)) {
            AirMapStatusAdvisory header = new AirMapStatusAdvisory();
            header.setName(YELLOW_TITLE);
            header.setId(HEADER_STRING); //Hack to have only a list of Advisories, but let it know it's a header
            advisories.add(header);
            for (AirMapStatusAdvisory advisory : data.get(YELLOW_TITLE)) {
                advisories.add(advisory);
            }
        }

        if (data.containsKey(GREEN_TITLE)) {
            AirMapStatusAdvisory header = new AirMapStatusAdvisory();
            header.setName(GREEN_TITLE);
            header.setId(HEADER_STRING); //Hack to have only a list of Advisories, but let it know it's a header
            advisories.add(header);
            for (AirMapStatusAdvisory advisory : data.get(GREEN_TITLE)) {
                advisories.add(advisory);
            }
        }

        organizationsMap = organizations;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_sheet_header_item, parent, false);
            return new VHHeader(view);
        } else if (viewType == TYPE_NORMAL) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_sheet_list_item, parent, false);
            return new VHItem(view);
        } else if (viewType == TYPE_TFR) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_sheet_tfr_item, parent, false);
            return new VHTfr(view);
        } else if (viewType == TYPE_WILDFIRE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_sheet_wildfire_item, parent, false);
            return new VHWildfire(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AirMapStatusAdvisory advisory = getItem(position);
        if (holder instanceof VHHeader) {
            onBindHeaderViewHolder((VHHeader) holder, advisory);
        } else if (holder instanceof VHItem) {
            onBindItemViewHolder((VHItem) holder, advisory);
        } else if (holder instanceof VHTfr) {
            onBindTfrViewHolder((VHTfr) holder, advisory);
        } else if (holder instanceof VHWildfire) {
            onBindWildfireViewHolder((VHWildfire) holder, advisory);
        }
    }


    private void onBindHeaderViewHolder(VHHeader holder, AirMapStatusAdvisory advisory) {
        holder.headerTextView.setText(advisory.getName().toUpperCase());
    }

    private void onBindItemViewHolder(final VHItem holder, final AirMapStatusAdvisory advisory) {
        holder.nameTextView.setText(advisory.getName());
        holder.phoneTextView.setText(advisory.getType().getTitle());
        holder.phoneTextView.setVisibility(View.VISIBLE);

        try {
            boolean showOrgName = false;
            if (!TextUtils.isEmpty(advisory.getOrganizationId())) {
                String orgName = organizationsMap.get(advisory.getOrganizationId());

                if (!TextUtils.isEmpty(orgName) && !orgName.equals(advisory.getName())) {
                    showOrgName = true;
                    holder.nameTextView.setText(orgName);
                    holder.organizationTextView.setText(advisory.getName());
                }
            }
            holder.organizationTextView.setVisibility(showOrgName ? View.VISIBLE : View.GONE);

            final String number = advisory.getAirportProperties().getPhone();
            if (number != null && number.length() >= 10) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(holder.itemView.getContext())
                                .setTitle(advisory.getName())
                                .setMessage(String.format(Locale.getDefault(), "Do you want to call %s?", advisory.getName()))
                                .setPositiveButton(R.string.call, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
                                        final Context context = holder.phoneTextView.getContext();
                                        if (intent.resolveActivity(context.getPackageManager()) != null) {
                                            context.startActivity(intent); //Only start activity if the device has a phone (e.g. A tablet might not)
                                        } else {
                                            holder.phoneTextView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(context, "No dialer found on device", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    }
                });
            } else {
                holder.itemView.setOnClickListener(null);
            }
        } catch (NullPointerException e) {
            holder.itemView.setOnClickListener(null);
        }

        holder.colorView.setBackgroundColor(getColor(advisory.getColor()));
    }

    private void onBindTfrViewHolder(final VHTfr holder, final AirMapStatusAdvisory advisory) {
        holder.colorView.setBackgroundColor(getColor(advisory.getColor()));
        holder.nameTextView.setText(advisory.getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.US);
        StringBuilder builder = new StringBuilder();
        if (advisory.getTfrProperties().getStartTime() != null) {
            builder.append(dateFormat.format(advisory.getTfrProperties().getStartTime()));
        }
        if (advisory.getTfrProperties().getEndTime() != null) {
            if (builder.length() != 0) { //Only add dash if we added a start time
                builder.append(" - ");
            }
            builder.append(dateFormat.format(advisory.getTfrProperties().getEndTime()));
        }
        holder.dateTextView.setText(builder.toString());
        String url = advisory.getTfrProperties().getUrl();
        if (url != null && !url.isEmpty()) {
            holder.urlTextView.setText(url);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(advisory.getTfrProperties().getUrl()));
                    holder.itemView.getContext().startActivity(intent);
                }
            });
            holder.urlTextView.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setOnClickListener(null);
            holder.urlTextView.setText(null);
            holder.urlTextView.setVisibility(View.GONE);
        }

    }

    private void onBindWildfireViewHolder(VHWildfire holder, AirMapStatusAdvisory advisory) {
        int size = advisory.getWildfireProperties().getSize();
        holder.colorView.setBackgroundColor(getColor(advisory.getColor()));
        holder.nameTextView.setText(advisory.getName());
        String unknownSize = holder.itemView.getContext().getString(R.string.unknown_size);
        holder.sizeTextView.setText(advisory.getType().getTitle() + " - " + (size == -1 ? unknownSize : String.format(Locale.US, "%d acres", size)));
    }

    private AirMapStatusAdvisory getItem(int position) {
        return advisories.get(position);
    }

    @Override
    public int getItemCount() {
        return advisories.size();
    }

    @Override
    public int getItemViewType(int position) {
        AirMapStatusAdvisory advisory = getItem(position);
        if (advisory.getId().equals(HEADER_STRING)) {
            return TYPE_HEADER;
        } else if (advisory.getTfrProperties() != null) {
            return TYPE_TFR;
        } else if (advisory.getWildfireProperties() != null) {
            return TYPE_WILDFIRE;
        }
        return TYPE_NORMAL;
    }

    private int getColor(AirMapStatus.StatusColor statusColor) {
        switch (statusColor) {
            case Red: {
                return ContextCompat.getColor(context, R.color.red);
            }
            case Yellow: {
                return ContextCompat.getColor(context, R.color.yellow);
            }
            case Green:
            default: {
                return ContextCompat.getColor(context, R.color.green);
            }
        }
    }

    public class VHHeader extends RecyclerView.ViewHolder {
        TextView headerTextView;

        public VHHeader(View itemView) {
            super(itemView);

            headerTextView = (TextView) itemView.findViewById(R.id.header);
        }
    }

    public class VHItem extends RecyclerView.ViewHolder {
        View colorView;
        TextView nameTextView;
        TextView organizationTextView;
        TextView phoneTextView;

        public VHItem(View itemView) {
            super(itemView);

            colorView = itemView.findViewById(R.id.color_bar);
            nameTextView = (TextView) itemView.findViewById(R.id.name);
            organizationTextView = (TextView) itemView.findViewById(R.id.organization);
            phoneTextView = (TextView) itemView.findViewById(R.id.phone);
        }

    }

    public class VHTfr extends RecyclerView.ViewHolder {
        View colorView;
        TextView nameTextView;
        TextView dateTextView;
        TextView urlTextView;

        public VHTfr(View itemView) {
            super(itemView);

            colorView = itemView.findViewById(R.id.color_bar);
            nameTextView = (TextView) itemView.findViewById(R.id.name);
            dateTextView = (TextView) itemView.findViewById(R.id.date);
            urlTextView = (TextView) itemView.findViewById(R.id.url);
        }

    }

    public class VHWildfire extends RecyclerView.ViewHolder {
        View colorView;
        TextView nameTextView;
        TextView sizeTextView;

        public VHWildfire(View itemView) {
            super(itemView);

            colorView = itemView.findViewById(R.id.color_bar);
            nameTextView = (TextView) itemView.findViewById(R.id.name);
            sizeTextView = (TextView) itemView.findViewById(R.id.size);
        }

    }
}