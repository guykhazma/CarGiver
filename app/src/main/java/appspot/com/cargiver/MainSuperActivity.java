package appspot.com.cargiver;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

public class MainSuperActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make sure user is logged if not redirect to start activity to handle this
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(getBaseContext(), StartActivity.class));
            finish();
        }

        // Set sender name for notifications.
        if(user.getDisplayName() == null || (user.getDisplayName() != null && user.getDisplayName().equals(""))){
            NotificationService.setSender(user.getEmail());
        }
        else{
            NotificationService.setSender(user.getDisplayName());
        }

        setContentView(R.layout.activity_main_super);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_super);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_super);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_super);
        navigationView.setNavigationItemSelectedListener(this);
        // set menu as selected on startup
        navigationView.getMenu().getItem(0).setChecked(true);

        /*-------------------- Main Fragment initialization --------------------------------------------*/
        // if we are just starting
        if (savedInstanceState == null) {
            // Create Main Fragment
            MainSuperFragment main = new MainSuperFragment();
            // load default activity
            getFragmentManager().beginTransaction().replace(R.id.fragment_container_super,main).commit();
        }

        // Fill current user details
        View navHeaderView= navigationView.getHeaderView(0);
        TextView txt = (TextView) navHeaderView.findViewById(R.id.name);
        txt.setText(user.getDisplayName());
        TextView email = (TextView) navHeaderView.findViewById(R.id.email);
        email.setText(user.getEmail());
        // load image
        ImageView img = (ImageView) navHeaderView.findViewById(R.id.imageView);
        Picasso.with(getBaseContext()).load(user.getPhotoUrl()).into(img);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_super);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_super, menu);
        menu.clear();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_main_supervisor) {
            // pop back fragments till reaching menu
            getFragmentManager().popBackStackImmediate(null,FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // Redirect to navigation main
        } else if (id == R.id.nav_drives_supervisor) {
            this.setTitle("Routes List");
            // pop back fragments till reaching menu
            getFragmentManager().popBackStackImmediate(null,FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // Redirect to manage drives fragment
            // open list of routes
            Fragment RoutesListFragmentSuper = new RoutesListFragmentSuper();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container_super, RoutesListFragmentSuper, "Drives List");
            // add to stack to allow return to menu on back press
            transaction.addToBackStack(null);
            transaction.commit();
            this.setTitle("Routes List");

        } else if (id == R.id.nav_manage_drivers_supervisor) {
            this.setTitle("Manage Drivers");
            // pop back fragments till reaching menu
            getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // Redirect to manage drives fragment
            // open list of routes
            Fragment ManageDriversFragment = new ManageDriversFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container_super, ManageDriversFragment, "Manage Drivers");
            // add to stack to allow return to menu on back press
            transaction.addToBackStack(null);
            transaction.commit();
            this.setTitle("Manage Drivers");

            // Redirect to manage drivers fragment
        } else if (id == R.id.nav_sign_out_driver) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        public void onComplete(@NonNull Task<Void> task) {
                            // user is now signed out
                            startActivity(new Intent(getBaseContext(), LoginActivity.class));
                            finish();
                        }
                    });
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_super);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
