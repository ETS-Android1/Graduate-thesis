package paket.projekat;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;
    RecyclerView recyclerViewPost;
    List<PostData> postList;
    List<FriendsData> friendsList;
    CustomAdapter4 customAdapter4;
    boolean state;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        onlineStatus();

        firebaseAuth = FirebaseAuth.getInstance();

        recyclerViewPost = view.findViewById(R.id.recyclerViewListOfPosts);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewPost.setLayoutManager(layoutManager);

        friendsList = new ArrayList<>();

        databaseReference = FirebaseDatabase.getInstance().getReference("Friends").child("user_"+firebaseAuth.getCurrentUser().getUid());
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendsList.clear();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    FriendsData friendsData = ds.getValue(FriendsData.class);
                    friendsList.add(friendsData);
                }
                loadPosts();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        //loadPosts();

        return view;
    }

    private void loadPosts() {
        postList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    PostData postData = ds.getValue(PostData.class);

                    for(FriendsData friendsData : friendsList) {
                        if(postData.getUserId() != null && (postData.getUserId().equals(friendsData.getId()) ||
                                postData.getUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))) {
                            postList.add(postData);
                            break;
                        }
                    }

                    if(friendsList.size() == 0) {
                        if(postData.getUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            postList.add(postData);
                        }
                    }

                    customAdapter4 = new CustomAdapter4(getContext(), postList);
                    recyclerViewPost.setAdapter(customAdapter4);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void searchPosts(String searchQuery) {
        postList = new ArrayList<>();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    PostData postData = ds.getValue(PostData.class);

                    if(postData.getPostTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            postData.getPostDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                        postList.add(postData);
                    }

                    customAdapter4 = new CustomAdapter4(getActivity(), postList);

                    recyclerViewPost.setAdapter(customAdapter4);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);    // za prikazivanje meni opcija u fragmentu

        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(!query.isEmpty()) {
                    searchPosts(query);
                }
                else {
                    loadPosts();
                }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if(!newText.isEmpty()) {
                    searchPosts(newText);
                }
                else {
                    loadPosts();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_logout) {
            offlineStatus();
            firebaseAuth.signOut();
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
        if(id == R.id.action_add_post) {
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void onlineStatus() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        try {
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("onlineStatus", "online");

            mDatabaseReference.updateChildren(hashMap);
        }
        catch (Exception e) {
        }
    }

    private void offlineStatus() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        try {
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("onlineStatus", "offline");

            mDatabaseReference.updateChildren(hashMap);
        }
        catch (Exception e) {
        }
    }

}