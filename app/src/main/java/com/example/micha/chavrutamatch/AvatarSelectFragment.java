package com.example.micha.chavrutamatch;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.example.micha.chavrutamatch.Data.AvatarImgs;

/**
 * Created by micha on 10/21/2017.
 */

public class AvatarSelectFragment extends Fragment {
    AvatarSelectAdapter mAdapter;
    OnAvatarClickListener mCallback;
    OnAvatarClickListener lowSdkCallback;
    Context mContext;
    Context mActivityContext;

    public interface OnAvatarClickListener {
        void onAvatarClick(int position);
    }

    // Override onAttach to make sure that the container activity has implemented the callback
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        // makes sure that the host activity has implemented the callback interface
        try {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                mCallback = (OnAvatarClickListener) context;
            } else {
                mActivityContext = getActivity();
                lowSdkCallback = (OnAvatarClickListener) mContext;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnImageClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.avatar_select_frag, container, false);

        GridView gridView = rootView.findViewById(R.id.gv_avatar_list);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            mAdapter = new AvatarSelectAdapter(getContext(), AvatarImgs.getAllFramedAvatars());
        } else {
            mActivityContext = getActivity();
            mAdapter = new AvatarSelectAdapter(mActivityContext, AvatarImgs.getAllFramedAvatars());
        }
        gridView.setAdapter(mAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //@conditional: only null if SDK version <23 as doesn't support getContext()
                if(mCallback == null) {
                    try {
                        lowSdkCallback = (OnAvatarClickListener) mActivityContext;
                    } catch (ClassCastException e) {
                        throw new ClassCastException(mActivityContext.toString()
                                + " must implement OnImageClickListener");
                    }
                    lowSdkCallback.onAvatarClick(position);
                } else {
                    mCallback.onAvatarClick(position);
                }
            }
        });
        return rootView;
    }

}
