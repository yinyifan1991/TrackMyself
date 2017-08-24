package com.trackmyself.yifan.trackmyself;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class StopTrackDialogFragment extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "title";
    private static final String ARG_PARAM2 = "message";


    public StopTrackDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param title dialog title.
     * @param message dialog message.
     * @return A new instance of fragment StopTrackDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StopTrackDialogFragment newInstance(String title, String message) {
        StopTrackDialogFragment fragment = new StopTrackDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, title);
        args.putString(ARG_PARAM2, message);
        fragment.setArguments(args);
        return fragment;
    }

    public interface StopTrackDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialopNegativeClick(DialogFragment dialog);
    }
    StopTrackDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            String title = getArguments().getString(ARG_PARAM1);
            String message = getArguments().getString(ARG_PARAM2);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(title).setMessage(message);
            alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i("onCreateDialog", "Yes");
                    mListener.onDialogPositiveClick(StopTrackDialogFragment.this);
                }
            });
            alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i("onCreateDialog","No");
                    mListener.onDialopNegativeClick(StopTrackDialogFragment.this);
                }
            });
            return alertDialogBuilder.create();
        }
        else return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView = new TextView(getActivity());
        textView.setText(R.string.hello_blank_fragment);
        return textView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (StopTrackDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement StopTrackDialogListener");
        }
    }
}
