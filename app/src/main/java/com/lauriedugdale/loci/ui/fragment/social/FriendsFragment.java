package com.lauriedugdale.loci.ui.fragment.social;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.lauriedugdale.loci.R;
import com.lauriedugdale.loci.data.UserDatabase;
import com.lauriedugdale.loci.data.dataobjects.User;
import com.lauriedugdale.loci.listeners.UsersDownloadedListener;
import com.lauriedugdale.loci.ui.activity.social.UserProfileActivity;
import com.lauriedugdale.loci.ui.adapter.social.FriendsAdapter;
import com.lauriedugdale.loci.ui.fragment.SocialFragment;

/**
 * @author Laurie Dugdale
 */

public class FriendsFragment extends Fragment implements FriendsAdapter.SocialAdapterOnClickHandler, UsersDownloadedListener {

    private FriendsAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private UserDatabase mUserDatabase;
    private ProgressBar mProgressBar;

    public static FriendsFragment create(){
        return new FriendsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        setHasOptionsMenu(true);

        mUserDatabase = new UserDatabase(getActivity());

        mProgressBar = (ProgressBar) view.findViewById(R.id.loading_indicator);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv_friends);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new FriendsAdapter(getActivity(), this, true);
        mRecyclerView.setAdapter(mAdapter);

        mUserDatabase.downloadUserFriends(mAdapter, this);
        return view;
    }


    @Override
    /**
     * On click method when an item in the recyclerview is clicked this launches the ViewEntryActivity class
     * passes the Uri of the clicked entry
     */
    public void onSocialClick(User user) {
        Intent startViewEntryIntent = new Intent(getActivity(), UserProfileActivity.class);
        startViewEntryIntent.putExtra(Intent.ACTION_OPEN_DOCUMENT, user);
        getActivity().startActivity(startViewEntryIntent);
    }

    @Override
    public void onUsersDownloaded() {

        mProgressBar.setVisibility(View.INVISIBLE);
    }
}
