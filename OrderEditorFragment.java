package com.wosaku.android.ordercontrol;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.wosaku.android.ordercontrol.data.OrdersContract;
import com.wosaku.android.ordercontrol.my_util.FileUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Wilian on 2016-12-12.
 * Order editor
 */
public class OrderEditorFragment extends Fragment implements android.app.LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener {

    public final static String SUPPLIER_SEPARATOR = " | ";
    private final static int PRODUCT_ITEM_ID_INITIAL = 0;
    private final static int PRODUCT_ITEM_ADD = 10;
    private final static int PRODUCT_ITEM_NUMBER_IDS = 1;
    private final static int PRODUCT_SPINNER_IDS = 2;
    private final static int PRODUCT_PRICE_IDS = 3;
    private final static int PRODUCT_QUANTITY_IDS = 4;
    private final static int PRODUCT_TOTALS_IDS = 5;
    private final static int PRODUCT_ID = 6;
    private final static int PRODUCT_ORDER_ID = 7;
    private final static String TIME_SUFFIX = " 23:59:59";
    private final static int PDF_TOTAL_QUANTITY_ID = 1234;
    private final static int PDF_TOTAL_PRICE_ID = 5678;
    private final static int ORDER_LOADER = 0;
    public ImageView mImageView;
    private final ArrayList<Long> productOrdersRemoved = new ArrayList<>();
    private ArrayAdapter<String> supplierArrayAdapter;
    private boolean addProductClicked = false;
    private boolean orientationChanged = false;
    private boolean replicateOrderSelector = false;
    private final Calendar myCalendar = Calendar.getInstance();
    private Cursor deliveryList;
    private Cursor orderProductCursor;
    private Cursor paymentList;
    private Cursor productList;
    private final DateFormat dateFormat = DateFormat.getDateInstance();
    private double productCostTotalDouble;
    private EditText commentsEditText;
    private EditText deliveryDateEditText;
    private EditText orderDateEditText;
    private EditText paymentDateEditText;
    private TextView totalPriceEditText;
    private TextView totalQuantityEditText;
    private long productCostInt;
    private long productCostTotalInt;
    private int productCount = 1;
    private int productItemId = 0;
    private long productQuantityInt;
    private int productSaved;
    private long totalQuantity = 0;
    private LinearLayout btCallSupplier;
    private LinearLayout btSendEmail;
    private LinearLayout productsContainer;
    private Long loadedOrderId;
    private long newOrderId;
    private Long orderProductId;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
    private final NumberFormat numberFormatter = NumberFormat.getNumberInstance();
    private OnCreateNewProductSelectedListener mCallback;
    private SimpleCursorAdapter spinnerDeliveryAdapter;
    private SimpleCursorAdapter spinnerPaymentAdapter;
    private SimpleCursorAdapter spinnerProductAdapter;
    private final SimpleDateFormat dateFormatSql = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private Spinner spinnerDeliveryStatus;
    private Spinner spinnerPaymentStatus;
    private Spinner spinnerProductItem;
    private Spinner spinnerSupplier;
    private String commentsString;
    private String deliveryDateString;
    private String mCurrentImagePath = null;
    private String orderDateString;
    private String paymentDateString;
    private String selectedDelivery;
    private String selectedPayment;
    private String selectedSupplier;
    private String selectedSupplierWithName;
    private String supplierEmail;
    private String supplierPhone;
    private Uri currentOrderUri;

    private TextView pdfOrderNumberTv;
    private TextView pdfSupplierNameTv;
    private TextView pdfOrderDateTv;
    private TextView pdfDeliveryDateTv;
    private TextView pdfPaymentDateTv;
    private TextView pdfComments;
    private String pdfSupName;


    private TableLayout pdfTable;
    private int pdfProductItemId = 1000;
    private String pdfOrdNum;
    public View pdfContent;

    private boolean isNewProduct = true;
    private int newProductItemCount = 0;


    // Check if a field was touched
    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            HomeActivity.mFragmentEditorHasChanged = true;
            return false;
        }
    };

    public OrderEditorFragment() {
        // required constructor
    }

    @TargetApi(23)
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (OnCreateNewProductSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnCreateNewProductSelectedListener");
        }
    }

    /*
     * Deprecated on API 23
     * Use onAttachToContext instead
     */
    @SuppressWarnings("deprecation")
    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < 23) {
            try {
                mCallback = (OnCreateNewProductSelectedListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement OnCreateNewProductSelectedListener");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_order_editor, container, false);

        // Find all elements
        orderDateEditText = (EditText) rootView.findViewById(R.id.et_order_date);
        spinnerSupplier = (Spinner) rootView.findViewById(R.id.spinner_order_supplier);
        productsContainer = (LinearLayout) rootView.findViewById(R.id.ll_products_container);
        Button btAddProduct = (Button) rootView.findViewById(R.id.bt_addItem);
        Button newProduct = (Button) rootView.findViewById(R.id.bt_addNewProduct);
        deliveryDateEditText = (EditText) rootView.findViewById(R.id.et_order_delivery_date);
        paymentDateEditText = (EditText) rootView.findViewById(R.id.et_order_payment_date);
        commentsEditText = (EditText) rootView.findViewById(R.id.et_order_comments);
        spinnerDeliveryStatus = (Spinner) rootView.findViewById(R.id.spinner_delivery_status);
        spinnerPaymentStatus = (Spinner) rootView.findViewById(R.id.spinner_payment_status);
        totalPriceEditText = (TextView) rootView.findViewById(R.id.et_total_price);
        totalQuantityEditText = (TextView) rootView.findViewById(R.id.et_total_quantity);
        mImageView = (ImageView) rootView.findViewById(R.id.order_image);
        Button btAddImage = (Button) rootView.findViewById(R.id.bt_addImage);
        Button btRemoveImage = (Button) rootView.findViewById(R.id.bt_removeImage);
        ImageView clearDeliveryDate = (ImageView) rootView.findViewById(R.id.delivery_date_clear);
        ImageView clearPaymentDate = (ImageView) rootView.findViewById(R.id.payment_date_clear);
        btCallSupplier = (LinearLayout) rootView.findViewById(R.id.bt_call_supplier);
        btSendEmail = (LinearLayout) rootView.findViewById(R.id.bt_sendEmail);

        // Views for print
        LinearLayout printPdfBt = (LinearLayout) rootView.findViewById(R.id.bt_printPdf);
        pdfSupplierNameTv = (TextView) rootView.findViewById(R.id.pdf_supplier_name);
        pdfOrderNumberTv = (TextView) rootView.findViewById(R.id.pdf_order_number);
        pdfOrderDateTv = (TextView) rootView.findViewById(R.id.pdf_order_date);
        pdfDeliveryDateTv = (TextView) rootView.findViewById(R.id.pdf_delivery_date);
        pdfPaymentDateTv = (TextView) rootView.findViewById(R.id.pdf_payment_date);
        pdfComments = (TextView) rootView.findViewById(R.id.pdf_comments);
        pdfTable = (TableLayout) rootView.findViewById(R.id.product_table);
        pdfContent = rootView.findViewById(R.id.pdf_container);


        // Get args from ProductCatalogFragment
        Bundle bundle = getArguments();
        if (bundle != null) {
            currentOrderUri = Uri.parse(getArguments().getString("order_id"));
            loadedOrderId = ContentUris.parseId(currentOrderUri);
            loadOrderProductCursor();
            isNewProduct = false;
        }

        // Check if the Activity started from Main (edit product) or from add button (new product)
        if (currentOrderUri == null) {
            getActivity().setTitle(getString(R.string.order_editor_title_new_product));
            printPdfBt.setVisibility(View.GONE);
        } else {
            getActivity().setTitle(getString(R.string.order_editor_title_edit_product));
            if (savedInstanceState == null) {
                getLoaderManager().initLoader(ORDER_LOADER, null, this);
            }
        }

        // Check if PDF should be enable
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            printPdfBt.setVisibility(View.GONE);
            pdfContent.setVisibility(View.GONE);
        }

        // Loading spinners data from database
        loadSpinnerSupplierData();
        loadSpinnerPayment();
        loadSpinnerDelivery();

        // Set order date to current date

        dateFormatSql.setTimeZone(TimeZone.getDefault());
        orderDateEditText.setText(dateFormat.format(myCalendar.getTime()));
        orderDateString = dateFormatSql.format(myCalendar.getTime()) + TIME_SUFFIX;

        // Set spinners listeners
        spinnerSupplier.setOnItemSelectedListener(this);
        spinnerPaymentStatus.setOnItemSelectedListener(this);
        spinnerDeliveryStatus.setOnItemSelectedListener(this);

        // Set click listeners
        btAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.addImageRequest = HomeActivity.ACTION_REQUEST_IMAGE_ORDER;
                ((HomeActivity) getActivity()).checkImagePermission();
            }
        });

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mImageView.getDrawable() != null) {
                    FileUtils.openImageInGallery(mImageView, mCurrentImagePath, getActivity());
                }
            }
        });
        btRemoveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeImage();
            }
        });

        btAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newProductItemCount = newProductItemCount + 1;
                addProductClicked = true;
                loadProductData();
            }
        });

        clearDeliveryDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deliveryDateEditText.getText().clear();
                deliveryDateString = null;
            }
        });

        clearPaymentDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paymentDateEditText.getText().clear();
                paymentDateString = null;
            }
        });

        btCallSupplier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtils.callSupplier(supplierPhone, getActivity());
            }
        });

        btSendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtils.sendEmail(supplierEmail, getActivity());
            }
        });

        newProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.save_order));
                builder.setPositiveButton(getString(R.string.action_save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saveOrder();
                        mCallback.onNewProductSelected(selectedSupplierWithName, newOrderId);
                    }
                });
                builder.setNegativeButton(getString(R.string.do_not_save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (dialog != null) {
                            dialog.dismiss();
                            mCallback.onNewProductSelected(selectedSupplierWithName, newOrderId);
                        }
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        printPdfBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HomeActivity.orderNumToPdf = pdfOrdNum;
                pdfLoadProductItems();
                ((HomeActivity) getActivity()).checkPdfPermission();
            }
        });


        final DatePickerDialog.OnDateSetListener orderDateDialog, deliveryDateDialog, paymentDateDialog;

        orderDateDialog = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                orderDateEditText.setText(dateFormat.format(myCalendar.getTime()));
                orderDateString = dateFormatSql.format(myCalendar.getTime()) + TIME_SUFFIX;

            }
        };

        deliveryDateDialog = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                deliveryDateEditText.setText(dateFormat.format(myCalendar.getTime()));
                deliveryDateString = dateFormatSql.format(myCalendar.getTime()) + TIME_SUFFIX;

            }
        };

        paymentDateDialog = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                paymentDateEditText.setText(dateFormat.format(myCalendar.getTime()));
                paymentDateString = dateFormatSql.format(myCalendar.getTime()) + TIME_SUFFIX;
            }
        };

        orderDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(getActivity(), orderDateDialog, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        deliveryDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(getActivity(), deliveryDateDialog, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        paymentDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(getActivity(), paymentDateDialog, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Set Touch listener to watch for possible edits and ask for confirmation when pressing back button
        orderDateEditText.setOnTouchListener(mTouchListener);
        spinnerSupplier.setOnTouchListener(mTouchListener);
        btAddProduct.setOnTouchListener(mTouchListener);
        deliveryDateEditText.setOnTouchListener(mTouchListener);
        spinnerDeliveryStatus.setOnTouchListener(mTouchListener);
        paymentDateEditText.setOnTouchListener(mTouchListener);
        spinnerPaymentStatus.setOnTouchListener(mTouchListener);
        commentsEditText.setOnTouchListener(mTouchListener);
        btAddImage.setOnTouchListener(mTouchListener);
        btRemoveImage.setOnTouchListener(mTouchListener);

        // Indicate that has options
        setHasOptionsMenu(true);

        // Set drawer indicator
        HomeActivity.mDrawerToggle.setDrawerIndicatorEnabled(false);

        return rootView;
    }

    private void loadOrderProductCursor(){

        String[] projection = {
                OrdersContract.OrderProductsEntry._ID,
                OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_ORD_ID,
                OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_ORD_ITEM_NUMBER,
                OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_ID,
                OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_PRICE,
                OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_QUANTITY
        };
        String whereClause = OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_ORD_ID + "=?";
        String[] whereArgs = {loadedOrderId.toString()};
        orderProductCursor = getActivity().getContentResolver().query(OrdersContract.OrderProductsEntry.CONTENT_URI, projection, whereClause, whereArgs, null);

        if (orderProductCursor != null && orderProductCursor.moveToFirst()) {
            productSaved = orderProductCursor.getCount();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Bundle handle automatically all text but not image
        mImageView = (ImageView) getActivity().findViewById(R.id.order_image);
        if (mImageView.getDrawable() != null && mImageView.getTag() != null) {
            mCurrentImagePath = mImageView.getTag().toString();
        }
        String savedImage = mCurrentImagePath;
        outState.putString("savedImage", savedImage);

        boolean savedMFragmentEditorHasChanged = HomeActivity.mFragmentEditorHasChanged;
        outState.putBoolean("savedMFragmentEditorHasChanged", savedMFragmentEditorHasChanged);

        // Used to save call supplier and send email visibility status
        int callSupplierVisible = btCallSupplier.getVisibility();
        outState.putInt("callSupplierVisible", callSupplierVisible);

        int sendEmailVisible = btSendEmail.getVisibility();
        outState.putInt("sendEmailVisible", sendEmailVisible);

        // Used to reload items
        String savedSupplier = selectedSupplier;
        outState.putString("savedSupplier", savedSupplier);

        String savedOrderNumber = pdfOrdNum;
        outState.putString("savedOrderNumber", savedOrderNumber);

        String savedSupplierName = pdfSupName;
        outState.putString("savedSupplierName", savedSupplierName);

        outState.putBoolean("orientationChanged", true);

        outState.putBoolean("isNewProduct", isNewProduct);

        outState.putInt("newProductItemCount", newProductItemCount);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            String savedImage = savedInstanceState.getString("savedImage");

            if (savedImage != null) {
                mImageView.setImageDrawable(new BitmapDrawable(getResources(),
                        FileUtils.getResizedBitmap(savedImage, 512, 512)));
                mImageView.setTag(savedImage);
            }
            HomeActivity.mFragmentEditorHasChanged = savedInstanceState.getBoolean("savedMFragmentEditorHasChanged");

            // Reshow call supplier and send email
            int callSupplierVisible = savedInstanceState.getInt("callSupplierVisible");
            switch (callSupplierVisible) {
                case View.VISIBLE:
                    btCallSupplier.setVisibility(View.VISIBLE);
                    break;
                case View.GONE:
                    btCallSupplier.setVisibility(View.GONE);
                    break;
            }

            int sendEmailVisible = savedInstanceState.getInt("sendEmailVisible");
            switch (sendEmailVisible) {
                case View.VISIBLE:
                    btSendEmail.setVisibility(View.VISIBLE);
                    break;
                case View.GONE:
                    btSendEmail.setVisibility(View.GONE);
                    break;
            }

            // Reload product items
            pdfOrdNum = savedInstanceState.getString("savedOrderNumber");
            pdfSupName = savedInstanceState.getString("savedSupplierName");

            selectedSupplier = savedInstanceState.getString("savedSupplier");
            orientationChanged = savedInstanceState.getBoolean("orientationChanged");

            isNewProduct = savedInstanceState.getBoolean("isNewProduct");
            newProductItemCount = savedInstanceState.getInt("newProductItemCount");

            if (!isNewProduct){
                loadOrderProductCursor();
                for (int i = 1; i <= productSaved; i++) {
                    loadProductData();
                    pdfCreateProductLines();
                    spinnerProductItem.setEnabled(false);
                }
                loadProductItem();
                pdfCreateTotalLine();
                pdfLoadProductItems();
            }

            if (isNewProduct && newProductItemCount > 0){
                for (int i = 1; i <= newProductItemCount; i++) {
                    loadProductData();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (productList != null) {
            productList.close();
        }

        if (paymentList != null) {
            paymentList.close();
        }
        if (deliveryList != null) {
            deliveryList.close();
        }
        if (orderProductCursor != null) {
            orderProductCursor.close();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_editor, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_delete:
                showDeleteConfirmationDialog();
                return true;
            case R.id.menu_action_replicate:
                replicateOrder();
                return true;
            case R.id.menu_action_save:
                saveOrder();
                return true;
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (currentOrderUri == null) {
            MenuItem actionDelete = menu.findItem(R.id.menu_action_delete);
            actionDelete.setVisible(false);

            MenuItem actionReplicate = menu.findItem(R.id.menu_action_replicate);
            actionReplicate.setVisible(false);
        }
    }


    private void removeImage() {
        mCurrentImagePath = null;
        mImageView.setTag(null);
        mImageView.setImageDrawable(null);
    }

    private void loadSpinnerSupplierData() {

        String[] projection = {
                OrdersContract.SupplierEntry._ID,
                OrdersContract.SupplierEntry.COLUMN_SUPPLIER_STATUS,
                OrdersContract.SupplierEntry.COLUMN_SUPPLIER_NAME
        };

        String whereClause = OrdersContract.SupplierEntry.COLUMN_SUPPLIER_STATUS + "=?";
        String[] whereArgs = {Integer.toString(OrdersContract.SupplierEntry.STATUS_ACTIVE)};

        Cursor supplierList;

        if (currentOrderUri != null) {
            supplierList = getActivity().getContentResolver().query(OrdersContract.SupplierEntry.CONTENT_URI, projection, null, null, null);
        } else {
            supplierList = getActivity().getContentResolver().query(OrdersContract.SupplierEntry.CONTENT_URI, projection, whereClause, whereArgs, null);
        }

        final ArrayList<String> supplierIdAndNameArray = new ArrayList<>();
        if (supplierList != null && supplierList.moveToFirst()) {
            while (!supplierList.isAfterLast()) {
                String supId = supplierList.getString(supplierList.getColumnIndexOrThrow(OrdersContract.SupplierEntry._ID));
                String supName = supplierList.getString(supplierList.getColumnIndexOrThrow(OrdersContract.SupplierEntry.COLUMN_SUPPLIER_NAME));
                String nameAndId = supName + SUPPLIER_SEPARATOR + supId;
                supplierIdAndNameArray.add(nameAndId);
                supplierList.moveToNext();
            }
            supplierList.close();
        }
        Collections.sort(supplierIdAndNameArray, String.CASE_INSENSITIVE_ORDER);
        supplierArrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_spinner, R.id.tvDBViewRow, supplierIdAndNameArray);
        spinnerSupplier.setAdapter(supplierArrayAdapter);
    }

    private void loadSpinnerPayment() {
        String[] projection = {
                OrdersContract.PaymentEntry._ID,
                OrdersContract.PaymentEntry.COLUMN_PAYMENT_STATUS,
                OrdersContract.PaymentEntry.COLUMN_PAYMENT_DELETED
        };
        String whereClause = OrdersContract.PaymentEntry.COLUMN_PAYMENT_DELETED + "=?";
        String[] whereArgs = {Integer.toString(OrdersContract.PaymentEntry.STATUS_ACTIVE)};
        paymentList = getActivity().getContentResolver().query(OrdersContract.PaymentEntry.CONTENT_URI, projection, whereClause, whereArgs, null);
        String[] from = new String[]{OrdersContract.PaymentEntry.COLUMN_PAYMENT_STATUS};
        int[] to = new int[]{R.id.tvDBViewRow};
        spinnerPaymentAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_spinner, paymentList, from, to, 0);
        spinnerPaymentStatus.setAdapter(spinnerPaymentAdapter);
    }

    private void loadSpinnerDelivery() {
        String[] projection = {
                OrdersContract.DeliveryEntry._ID,
                OrdersContract.DeliveryEntry.COLUMN_DELIVERY_STATUS,
                OrdersContract.DeliveryEntry.COLUMN_DELIVERY_DELETED
        };
        String whereClause = OrdersContract.DeliveryEntry.COLUMN_DELIVERY_DELETED + "=?";
        String[] whereArgs = {Integer.toString(OrdersContract.DeliveryEntry.STATUS_ACTIVE)};
        deliveryList = getActivity().getContentResolver().query(OrdersContract.DeliveryEntry.CONTENT_URI, projection, whereClause, whereArgs, null);
        String[] from = new String[]{OrdersContract.DeliveryEntry.COLUMN_DELIVERY_STATUS};
        int[] to = new int[]{R.id.tvDBViewRow};
        spinnerDeliveryAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_spinner, deliveryList, from, to, 0);
        spinnerDeliveryStatus.setAdapter(spinnerDeliveryAdapter);
    }

    private void loadProductData() {

        spinnerSupplier.setEnabled(false);
        String[] projection = {
                OrdersContract.ProductEntry.TABLE_NAME + "." + OrdersContract.ProductEntry._ID,
                OrdersContract.ProductEntry.COLUMN_PRODUCT_NAME,
                OrdersContract.ProductEntry.COLUMN_PRODUCT_STATUS,
                OrdersContract.ProductEntry.COLUMN_SUPPLIER_ID,
                OrdersContract.ProductEntry.COLUMN_PRODUCT_PRICE
        };
        String orderBy = OrdersContract.ProductEntry.TABLE_NAME + "." + OrdersContract.ProductEntry.COLUMN_PRODUCT_NAME + " ASC";

        if (currentOrderUri != null) {
            String whereClause = OrdersContract.ProductEntry.COLUMN_SUPPLIER_ID + "=?";
            String[] whereArgs = {selectedSupplier};
            productList = getActivity().getContentResolver().query(OrdersContract.ProductEntry.CONTENT_URI, projection, whereClause, whereArgs, orderBy);
        }

        if (currentOrderUri == null || addProductClicked) {
            String whereClause = OrdersContract.ProductEntry.COLUMN_SUPPLIER_ID + "=?" + " AND " + OrdersContract.ProductEntry.COLUMN_PRODUCT_STATUS + "=?";
            String[] whereArgs = {selectedSupplier, "1"};
            productList = getActivity().getContentResolver().query(OrdersContract.ProductEntry.CONTENT_URI, projection, whereClause, whereArgs, orderBy);
        }

        if (productList != null && !productList.moveToFirst()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.no_product_found);
            builder.setPositiveButton(R.string.create_product, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    saveOrder();
                    mCallback.onNewProductSelected(selectedSupplierWithName, newOrderId);
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            });

            // Create and show the AlertDialog
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return;
        }
        createProductLine();
    }


    private void createProductLine() {
        String[] from = new String[]{OrdersContract.ProductEntry.COLUMN_PRODUCT_NAME};
        int[] to = new int[]{R.id.tvDBViewRow};
        spinnerProductAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_spinner, productList, from, to, 0);

        spinnerProductItem = new Spinner(getActivity());
        spinnerProductItem.setOnItemSelectedListener(this);
        spinnerProductItem.setAdapter(spinnerProductAdapter);

        productItemId = productItemId + PRODUCT_ITEM_ADD;
        LinearLayout productItemLine = new LinearLayout(getActivity());
        productItemLine.setPadding(8,8,8,8);
        productItemLine.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams productItemLineParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        productItemLine.setId(+productItemId);
        productItemLine.setLayoutParams(productItemLineParams);
        if (productItemId / PRODUCT_ITEM_ADD % 2 == 1) {
            productItemLine.setBackgroundResource(R.color.item1);
        } else {
            productItemLine.setBackgroundResource(R.color.item2);
        }

        productsContainer.addView(productItemLine);

        LinearLayout spinnerLine = new LinearLayout(getActivity());
        LinearLayout.LayoutParams spinnerLineParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        spinnerLine.setOrientation(LinearLayout.HORIZONTAL);
        spinnerLine.setLayoutParams(spinnerLineParams);
        productItemLine.addView(spinnerLine);


        LinearLayout itemNumberContainer = new LinearLayout(getActivity());
        LinearLayout.LayoutParams itemNumberContainerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 2.0f);
        itemNumberContainer.setLayoutParams(itemNumberContainerParams);
        spinnerLine.addView(itemNumberContainer);
        TextView productItemNumber = new TextView(getActivity());
        LinearLayout.LayoutParams productItemNumberParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        productItemNumber.setLines(1);
        productItemNumber.setGravity(Gravity.CENTER_VERTICAL);

        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (getResources().getDimension(R.dimen.item_number_padding_order_editor) * scale + 0.5f);

        productItemNumber.setMinWidth(pixels);
        productItemNumber.setLayoutParams(productItemNumberParams);
        productItemNumber.setText(String.format(Locale.ENGLISH, "%d)", productCount));

        productItemNumber.setId(+productItemId + PRODUCT_ITEM_NUMBER_IDS);
        productCount++;
        itemNumberContainer.addView(productItemNumber);


        LinearLayout spinnerContainer = new LinearLayout(getActivity());
        LinearLayout.LayoutParams spinnerContainerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 5.0f);
        spinnerContainer.setLayoutParams(spinnerContainerParams);
        spinnerLine.addView(spinnerContainer);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        spinnerProductItem.setLayoutParams(spinnerParams);
        spinnerContainer.addView(spinnerProductItem);
        spinnerProductItem.setId(+productItemId + PRODUCT_SPINNER_IDS);


        LinearLayout invisibleContainer = new LinearLayout(getActivity());
        LinearLayout.LayoutParams invisibleContainerContainerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 0.5f);
        invisibleContainer.setLayoutParams(invisibleContainerContainerParams);
        spinnerLine.addView(invisibleContainer);
        TextView productIdTextView = new TextView(getActivity());
        LinearLayout.LayoutParams invisibleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        productIdTextView.setTextSize(4);
        productIdTextView.setLayoutParams(invisibleParams);
        invisibleContainer.addView(productIdTextView);
        productIdTextView.setVisibility(View.INVISIBLE);
        productIdTextView.setId(+productItemId + PRODUCT_ID);
        TextView productOrderIdTextView = new TextView(getActivity());
        productOrderIdTextView.setTextSize(4);
        productOrderIdTextView.setLayoutParams(invisibleParams);
        invisibleContainer.addView(productOrderIdTextView);
        productOrderIdTextView.setVisibility(View.INVISIBLE);
        productOrderIdTextView.setId(+productItemId + PRODUCT_ORDER_ID);


        LinearLayout removeItemContainer = new LinearLayout(getActivity());
        LinearLayout.LayoutParams removeItemContainerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        removeItemContainer.setLayoutParams(removeItemContainerParams);
        spinnerLine.addView(removeItemContainer);
        final ImageView removeItem = new ImageView(getActivity());
        LinearLayout.LayoutParams removeImageParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        removeImageParams.setMargins(10, 4, 10, 4);
        removeItem.setLayoutParams(removeImageParams);
        removeItem.setImageResource(R.drawable.ic_remove_circle_outline_black_24dp);
        removeItemContainer.addView(removeItem);


        // Add a new horizontal line, will host price and quantity
        final LinearLayout priceLine = new LinearLayout(getActivity());
        priceLine.setOrientation(LinearLayout.HORIZONTAL);
        priceLine.setLayoutParams(productItemLineParams);
        productItemLine.addView(priceLine);

        final LinearLayout priceContainer = new LinearLayout(getActivity());
        priceContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelContainers = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        priceContainer.setLayoutParams(labelContainers);
        labelContainers.gravity = Gravity.CENTER_VERTICAL;
        labelContainers.setMargins(0, 0, 0, 0);

        priceLine.addView(priceContainer);


        TextView unitPriceLabel = new TextView(getActivity());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        unitPriceLabel.setLayoutParams(labelParams);
        unitPriceLabel.setText(getString(R.string.order_editor_product_symbol));
        priceContainer.addView(unitPriceLabel);

        TextView productPriceTextView = new TextView(getActivity());
        ViewGroup.LayoutParams productCostParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        productPriceTextView.setLayoutParams(productCostParams);
        productPriceTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_order_editor));

        productPriceTextView.setPadding(0,0,0,20);
        priceContainer.addView(productPriceTextView);
        productPriceTextView.setId(+productItemId + PRODUCT_PRICE_IDS);

        final LinearLayout quantityContainer = new LinearLayout(getActivity());
        LinearLayout.LayoutParams quantityContainerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);

        quantityContainer.setOrientation(LinearLayout.VERTICAL);
        quantityContainer.setLayoutParams(quantityContainerParams);
        priceLine.addView(quantityContainer);

        TextView qtyLabel = new TextView(getActivity());
        labelParams.gravity = Gravity.CENTER_VERTICAL;
        qtyLabel.setLayoutParams(labelParams);
        qtyLabel.setText(getString(R.string.order_editor_quantity_symbol));

        quantityContainer.addView(qtyLabel);

        EditText productQuantityEditText = new EditText(getActivity());
        ViewGroup.LayoutParams productQuantityTextViewParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        productQuantityEditText.setLayoutParams(productQuantityTextViewParams);
        productQuantityEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        productQuantityEditText.setPadding(0,0,0,20);
        productQuantityEditText.setText("1");
        productQuantityEditText.setGravity(Gravity.CENTER);
        productQuantityEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_order_editor));
        quantityContainer.addView(productQuantityEditText);
        productQuantityEditText.setId(+productItemId + PRODUCT_QUANTITY_IDS);

        LinearLayout totalContainer = new LinearLayout(getActivity());
        LinearLayout.LayoutParams totalContainerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

        totalContainer.setOrientation(LinearLayout.VERTICAL);
        totalContainer.setLayoutParams(totalContainerParams);
        priceLine.addView(totalContainer);

        TextView totalLabel = new TextView(getActivity());
        totalLabel.setLayoutParams(labelParams);
        totalLabel.setText(getString(R.string.order_editor_total_symbol));
        totalContainer.addView(totalLabel);

        TextView productPriceTotalTextView = new TextView(getActivity());
        ViewGroup.LayoutParams productCostTotalTextViewParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        productPriceTotalTextView.setLayoutParams(productCostTotalTextViewParams);
        productPriceTotalTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_order_editor));
        productPriceTotalTextView.setPadding(0,0,0,20);

        totalContainer.addView(productPriceTotalTextView);
        productPriceTotalTextView.setId(+productItemId + PRODUCT_TOTALS_IDS);

        removeItem.setOnClickListener(new View.OnClickListener() {
            final View getParent = (View) removeItem.getParent().getParent().getParent();

            @Override
            public void onClick(View view) {
                int currentProductItem = getParent.getId();
                int currentProductOrderId = currentProductItem + PRODUCT_ORDER_ID;
                TextView currentProductOrderTv = (TextView) getActivity().findViewById(currentProductOrderId);

                if (!currentProductOrderTv.getText().toString().equals("")) {
                    Long currentProductOrderLong = Long.parseLong(currentProductOrderTv.getText().toString());
                    productOrdersRemoved.add(currentProductOrderLong);
                }

                LinearLayout itemToRemove = (LinearLayout) getActivity().findViewById(currentProductItem);
                productsContainer.removeView(itemToRemove);
                updateTotalPrice();
                updateTotalQuantity();
                updateProductItemNumber();

                if (productCount == 1) {
                    totalPriceEditText.setText("0");
                    totalQuantityEditText.setText("0");
                    spinnerSupplier.setEnabled(true);
                    productItemId = 0;
                }
                newProductItemCount = newProductItemCount - 1;
            }
        });
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, View view, int pos, long l) {
        switch (parent.getId()) {
            case R.id.spinner_order_supplier:
                selectedSupplierWithName = parent.getItemAtPosition(pos).toString();
                selectedSupplier = selectedSupplierWithName.substring(selectedSupplierWithName.indexOf(SUPPLIER_SEPARATOR.trim()) + 1).trim();
                break;
            case R.id.spinner_delivery_status:
                deliveryList = (Cursor) parent.getSelectedItem();
                selectedDelivery = deliveryList.getString(deliveryList.getColumnIndexOrThrow(OrdersContract.DeliveryEntry._ID));
                break;
            case R.id.spinner_payment_status:
                paymentList = (Cursor) parent.getSelectedItem();
                selectedPayment = paymentList.getString(paymentList.getColumnIndexOrThrow(OrdersContract.PaymentEntry._ID));
                break;
            default:
                productList = (Cursor) parent.getSelectedItem();
                int selectedSpinner = parent.getId();
                int selectedProduct = selectedSpinner - PRODUCT_SPINNER_IDS;
                int productPriceId = selectedProduct + PRODUCT_PRICE_IDS;
                int productQuantityId = selectedProduct + PRODUCT_QUANTITY_IDS;
                int productTotalId = selectedProduct + PRODUCT_TOTALS_IDS;
                int productId = selectedProduct + PRODUCT_ID;

                TextView productCostItem = (TextView) getActivity().findViewById(productPriceId);
                final EditText productQuantityItem = (EditText) getActivity().findViewById(productQuantityId);
                final TextView productCostTotalItem = (TextView) getActivity().findViewById(productTotalId);
                TextView productIdTextView = (TextView) getActivity().findViewById(+productId);

                int selectedProductId = productList.getInt(productList.getColumnIndexOrThrow(OrdersContract.ProductEntry._ID));

                // If it is a new order OR if it is a loaded order but a new item has been added
                if (orderProductCursor == null || addProductClicked) {

                    String selectedProductPriceString = productList.getString(productList.getColumnIndexOrThrow(OrdersContract.ProductEntry.COLUMN_PRODUCT_PRICE));
                    long selectedProductPriceInt = Long.parseLong(selectedProductPriceString);
                    double selectedProductPriceDouble = selectedProductPriceInt / 100.00;
                    String currentProductPriceFormatted = currencyFormatter.format(selectedProductPriceDouble);
                    productCostItem.setText(currentProductPriceFormatted);

                    productCostInt = selectedProductPriceInt;
                    if (productQuantityItem.getText().toString().equals("") ||productQuantityItem.getText().toString().equals("0") ){
                        productQuantityInt = 0;
                    }
                    else {
                        productQuantityInt = Long.parseLong(productQuantityItem.getText().toString());
                    }
                    productCostTotalInt = productCostInt * productQuantityInt;
                    productCostTotalDouble = productCostTotalInt / 100.00;
                    String currentProductPriceTotalFormatted = currencyFormatter.format(productCostTotalDouble);
                    productCostTotalItem.setText(currentProductPriceTotalFormatted);
                }

                // If it is a loaded order, the price should be the saved price
                else {
                    if (!orientationChanged) {
                        productCostInt = FileUtils.currencyToInt(productCostItem.getText().toString());
                    }
                }

                productIdTextView.setText(String.valueOf(selectedProductId));

                updateTotalPrice();
                updateTotalQuantity();

                productQuantityItem.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (!"".equals(productQuantityItem.getText().toString())) {
                            int selectedId = 0;
                            if (getActivity().getCurrentFocus() != null) {
                                selectedId = getActivity().getCurrentFocus().getId() - PRODUCT_QUANTITY_IDS;
                            }
                            int priceToGet = selectedId + PRODUCT_PRICE_IDS;
                            TextView productCostItem = (TextView) getActivity().findViewById(priceToGet);
                            if (productCostItem != null) {
                                productCostInt = FileUtils.currencyToInt(productCostItem.getText().toString());
                                productQuantityInt = Long.parseLong(productQuantityItem.getText().toString());
                                productCostTotalInt = productCostInt * productQuantityInt;
                                productCostTotalDouble = productCostTotalInt / 100.00;
                                String currentProductPriceTotalFormatted = currencyFormatter.format(productCostTotalDouble);
                                productCostTotalItem.setText(currentProductPriceTotalFormatted);
                                updateTotalPrice();
                                updateTotalQuantity();
                            }
                        }
                    }
                });

                productQuantityItem.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        if (!hasFocus) {
                            if (productQuantityItem.getText().toString().equals("")) {
                                productQuantityItem.setText("1");
                                productQuantityInt = 1;
                                updateTotalPrice();
                                updateTotalQuantity();
                            }
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void updateTotalPrice() {
        long totalPriceInt = 0;
        double totalPriceDouble;
        for (int i = PRODUCT_ITEM_ID_INITIAL; i <= productItemId; i = i + PRODUCT_ITEM_ADD) {
            int currentTotalPrice = i + PRODUCT_TOTALS_IDS;
            TextView totalPrice = (TextView) getActivity().findViewById(+currentTotalPrice);

            if (totalPrice != null) {
                String totalPriceString = totalPrice.getText().toString();
                if (!"".equals(totalPriceString)) {
                    totalPriceInt = totalPriceInt + FileUtils.currencyToInt(totalPriceString);
                    totalPriceDouble = totalPriceInt / 100.00;
                    String totalPriceDoubleFormatted = currencyFormatter.format(totalPriceDouble);
                    totalPriceEditText.setText(totalPriceDoubleFormatted);
                }
            }
        }
    }

    private void updateTotalQuantity() {
        totalQuantity = 0;
        for (int i = PRODUCT_ITEM_ID_INITIAL; i <= productItemId; i = i + PRODUCT_ITEM_ADD) {
            int currentQuantity = i + PRODUCT_QUANTITY_IDS;
            EditText totalQuantityTextView = (EditText) getActivity().findViewById(+currentQuantity);
            if (totalQuantityTextView != null) {
                String totalQuantityString = totalQuantityTextView.getText().toString();
                if (!totalQuantityString.equals("") && !totalQuantityString.equals("0")){
                    totalQuantity = totalQuantity + Integer.parseInt(totalQuantityString);
                    totalQuantityEditText.setText(numberFormatter.format(totalQuantity));
                }
            }
        }
    }

    private void updateProductItemNumber() {
        productCount = 1;
        for (int i = PRODUCT_ITEM_ID_INITIAL; i <= productItemId; i = i + PRODUCT_ITEM_ADD) {
            int currentProductItem = i + PRODUCT_ITEM_NUMBER_IDS;
            TextView productItemNumberTextView = (TextView) getActivity().findViewById(+currentProductItem);
            if (productItemNumberTextView != null) {
                productItemNumberTextView.setText(String.format(Locale.ENGLISH, "%d)", productCount));
                productCount++;
            }
        }
    }

    // Save product
    private void saveOrder() {
        HomeActivity.mFragmentEditorHasChanged = false;

        // Get values from EditText
        commentsString = commentsEditText.getText().toString().trim();

        String priceString = FileUtils.textViewNumToCurrencyString(totalPriceEditText, getActivity());

        if (mImageView.getDrawable() != null && mImageView.getTag() != null) {
            mCurrentImagePath = mImageView.getTag().toString();
        }

        String totalQuantityString = totalQuantityEditText.getText().toString();

        ContentValues values = new ContentValues();
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_DATE, orderDateString);
        values.put(OrdersContract.OrderEntry.COLUMN_SUPPLIER_ID, selectedSupplier);
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_DELIVERY_DATE, deliveryDateString);
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_DELIVERY_STATUS, selectedDelivery);
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_PAYMENT_DATE, paymentDateString);
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_PAYMENT_STATUS, selectedPayment);
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_TOTAL_PRICE, priceString);
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_TOTAL_QUANTITY, totalQuantityString);
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_IMAGE_PATH, mCurrentImagePath);
        values.put(OrdersContract.OrderEntry.COLUMN_ORDER_COMMENTS, commentsString);

        // Check if it is a new product (null, use insert) or an existing product (else, use update)
        if (currentOrderUri == null) {
            Uri newUri = getActivity().getContentResolver().insert(OrdersContract.OrderEntry.CONTENT_URI, values);
            newOrderId = ContentUris.parseId(newUri);

            if (newUri == null) {
                Toast.makeText(getActivity(), getString(R.string.editor_insert_order_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.editor_insert_order_successful),
                        Toast.LENGTH_SHORT).show();
                saveOrderProducts();
            }

        } else {
            int rowsAffected = getActivity().getContentResolver().update(currentOrderUri, values, null, null);
            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(getActivity(), getString(R.string.editor_insert_order_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(getActivity(), getString(R.string.editor_insert_order_successful),
                        Toast.LENGTH_SHORT).show();
                saveOrderProducts();
            }
        }
        OrderCatalogFragment orderCatalogFragment = new OrderCatalogFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.popBackStack();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, orderCatalogFragment, HomeActivity.GO_TO_ROOT_TAG)
                .commit();
    }

    private void saveOrderProducts() {
        // Delete removed item if any
        deleteRemovedItem();
        for (int i = PRODUCT_ITEM_ID_INITIAL; i <= productItemId; i = i + PRODUCT_ITEM_ADD) {
            int currentItemNumber = i + PRODUCT_ITEM_NUMBER_IDS;
            int currentItemId = i + PRODUCT_ID;
            int currentItemPrice = i + PRODUCT_PRICE_IDS;
            int currentItemQuantity = i + PRODUCT_QUANTITY_IDS;
            int currentItemOrderProduct = i + PRODUCT_ORDER_ID;

            TextView currentItemNumberTv = (TextView) getActivity().findViewById(+currentItemNumber);
            TextView currentItemPriceTv = (TextView) getActivity().findViewById(+currentItemPrice);
            TextView currentItemIdTv = (TextView) getActivity().findViewById(+currentItemId);
            EditText currentItemQuantityEt = (EditText) getActivity().findViewById(+currentItemQuantity);
            TextView currentItemOrderProductTv = (TextView) getActivity().findViewById(currentItemOrderProduct);

            if (currentItemNumberTv != null && currentItemPriceTv != null && currentItemIdTv != null && currentItemQuantityEt != null) {
                String itemNumberString = currentItemNumberTv.getText().toString().replace(")", "");
                String itemIdString = currentItemIdTv.getText().toString();
                String itemQuantity = currentItemQuantityEt.getText().toString();

                if (!"".equals(currentItemOrderProductTv.getText().toString()) && !replicateOrderSelector) {
                    String currentItemOrderProductString = currentItemOrderProductTv.getText().toString();
                    orderProductId = Long.parseLong(currentItemOrderProductString);
                } else {
                    orderProductId = null;
                }

                long itemPrice = FileUtils.currencyToInt(currentItemPriceTv.getText().toString());
                String itemPriceString = Long.toString(itemPrice);

                // If it is an existing order
                if (currentOrderUri != null) {
                    newOrderId = loadedOrderId;
                }

                Uri currentOrderProductUri;

                // If it is an existing order with existing products
                if (orderProductId != null) {
                    currentOrderProductUri = ContentUris.withAppendedId(OrdersContract.OrderProductsEntry.CONTENT_URI, orderProductId);
                } else {
                    currentOrderProductUri = null;
                }

                // If is an existing order with NO products
                if (currentOrderUri != null && orderProductId == null) {
                    newOrderId = loadedOrderId;
                }

                ContentValues values = new ContentValues();
                values.put(OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_ORD_ID, newOrderId);
                values.put(OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_ORD_ITEM_NUMBER, itemNumberString);
                values.put(OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_ID, itemIdString);
                values.put(OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_PRICE, itemPriceString);
                values.put(OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_QUANTITY, itemQuantity);

                // Check if it is a new orderProduct (null, use insert) or an existing product (else, use update)
                if (currentOrderProductUri == null) {
                    getActivity().getContentResolver().insert(OrdersContract.OrderProductsEntry.CONTENT_URI, values);
                } else {
                    getActivity().getContentResolver().update(currentOrderProductUri, values, null, null);
                }
            }
        }
    }

    private void deleteRemovedItem() {
        if (!productOrdersRemoved.isEmpty()) {
            for (int i = 0; i < productOrdersRemoved.size(); i++) {
                Uri currentOrderProductUriToDelete = ContentUris.withAppendedId(OrdersContract.OrderProductsEntry.CONTENT_URI, productOrdersRemoved.get(i));
                getActivity().getContentResolver().delete(currentOrderProductUriToDelete, null, null);
            }
        }
    }


    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.delete_order_confirmation));
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteOrder();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteOrder() {
        if (orderProductCursor.getCount() > 0) {
            orderProductCursor.moveToFirst();
            for (int i = 0; i < orderProductCursor.getCount(); i++) {
                String currentOrderProductString = orderProductCursor.getString(orderProductCursor.getColumnIndexOrThrow(OrdersContract.OrderProductsEntry._ID));
                Uri currentOrderProductToDelete = ContentUris.withAppendedId(OrdersContract.OrderProductsEntry.CONTENT_URI, Long.parseLong(currentOrderProductString));
                getActivity().getContentResolver().delete(currentOrderProductToDelete, null, null);
                orderProductCursor.moveToNext();
            }
        }
        int mRowsDeleted = getActivity().getContentResolver().delete(currentOrderUri, null, null);
        if (mRowsDeleted == 0) {
            // If no rows were affected, then there was an error with the update.
            Toast.makeText(getActivity(), getString(R.string.editor_delete_order_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(getActivity(), getString(R.string.editor_delete_order_successful),
                    Toast.LENGTH_SHORT).show();
        }
        getFragmentManager().popBackStackImmediate();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        CursorLoader loader = null;
        String[] projection;

        switch (id) {
            case ORDER_LOADER:
                // Load the columns in the projection
                projection = new String[]{
                        OrdersContract.OrderEntry.TABLE_NAME + "." + OrdersContract.OrderEntry._ID,
                        OrdersContract.OrderEntry.COLUMN_ORDER_DATE,
                        OrdersContract.OrderEntry.COLUMN_SUPPLIER_ID,
                        OrdersContract.SupplierEntry.COLUMN_SUPPLIER_NAME,
                        OrdersContract.SupplierEntry.COLUMN_SUPPLIER_PHONE,
                        OrdersContract.SupplierEntry.COLUMN_SUPPLIER_EMAIL,
                        OrdersContract.OrderEntry.COLUMN_ORDER_DELIVERY_DATE,
                        OrdersContract.OrderEntry.COLUMN_ORDER_DELIVERY_STATUS,
                        OrdersContract.OrderEntry.COLUMN_ORDER_PAYMENT_DATE,
                        OrdersContract.OrderEntry.COLUMN_ORDER_PAYMENT_STATUS,
                        OrdersContract.OrderEntry.COLUMN_ORDER_COMMENTS,
                        OrdersContract.OrderEntry.COLUMN_ORDER_TOTAL_PRICE,
                        OrdersContract.OrderEntry.COLUMN_ORDER_TOTAL_QUANTITY,
                        OrdersContract.OrderEntry.COLUMN_ORDER_IMAGE_PATH
                };
                loader = new CursorLoader(getActivity(), currentOrderUri, projection, null, null, null);
                break;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor.moveToFirst()) {
            orderDateString = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_DATE));
            try {
                Date orderDateFormatted = dateFormatSql.parse(orderDateString);
                orderDateEditText.setText(dateFormat.format(orderDateFormatted));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String supId = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_SUPPLIER_ID));
            String supName = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.SupplierEntry.COLUMN_SUPPLIER_NAME));
            String nameAndId = supName + SUPPLIER_SEPARATOR + supId;

            pdfSupName = supName;

            for (int pos = supplierArrayAdapter.getCount() - 1; pos >= 0; pos--) {
                if (supplierArrayAdapter.getItem(pos).equals(nameAndId)) {
                    spinnerSupplier.setSelection(pos);
                    break;
                }
            }

            selectedSupplier = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_SUPPLIER_ID));

            for (int i = 1; i <= productSaved; i++) {
                loadProductData();
                spinnerProductItem.setEnabled(false);
                pdfCreateProductLines();
            }
            pdfCreateTotalLine();


            deliveryDateString = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_DELIVERY_DATE));
            if (!TextUtils.isEmpty(deliveryDateString)) {
                try {
                    Date dateFormatted = dateFormatSql.parse(deliveryDateString);
                    deliveryDateEditText.setText(dateFormat.format(dateFormatted));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            int paymentIdCheck = cursor.getInt(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_PAYMENT_STATUS));
            if (paymentIdCheck > 0) {
                for (int pos = spinnerPaymentAdapter.getCount(); pos >= 0; pos--) {
                    if (spinnerPaymentAdapter.getItemId(pos) == paymentIdCheck) {
                        spinnerPaymentStatus.setSelection(pos);
                        break;
                    }
                }
            }

            int deliveryIdCheck = cursor.getInt(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_DELIVERY_STATUS));
            if (deliveryIdCheck > 0) {
                for (int pos = spinnerDeliveryAdapter.getCount(); pos >= 0; pos--) {
                    if (spinnerDeliveryAdapter.getItemId(pos) == deliveryIdCheck) {
                        spinnerDeliveryStatus.setSelection(pos);
                        break;
                    }
                }
            }


            paymentDateString = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_PAYMENT_DATE));
            if (!TextUtils.isEmpty(paymentDateString)) {
                try {
                    Date dateFormatted = dateFormatSql.parse(paymentDateString);
                    paymentDateEditText.setText(dateFormat.format(dateFormatted));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            commentsString = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_COMMENTS));
            if (commentsString != null) {
                commentsEditText.setText(commentsString);
            }

            mCurrentImagePath = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_IMAGE_PATH));
            if (mCurrentImagePath != null && !mCurrentImagePath.isEmpty()) {
                File fileCheck = new File(mCurrentImagePath);
                if (fileCheck.exists()) {
                    mImageView.setImageDrawable(new BitmapDrawable(getResources(),
                            FileUtils.getResizedBitmap(mCurrentImagePath, 512, 512)));
                }
            }

            long currentProductPrice = cursor.getLong(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_TOTAL_PRICE));
            double currentProductPriceDouble = currentProductPrice / 100.00;

            String currentTotalPriceFormatted = currencyFormatter.format(currentProductPriceDouble);
            totalPriceEditText.setText(currentTotalPriceFormatted);

            totalQuantity = cursor.getInt(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry.COLUMN_ORDER_TOTAL_QUANTITY));
            totalQuantityEditText.setText(numberFormatter.format(totalQuantity));

            supplierPhone = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.SupplierEntry.COLUMN_SUPPLIER_PHONE));
            if (!TextUtils.isEmpty(supplierPhone)) {
                btCallSupplier.setVisibility(View.VISIBLE);
            } else {
                btCallSupplier.setVisibility(View.GONE);
            }

            supplierEmail = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.SupplierEntry.COLUMN_SUPPLIER_EMAIL));
            if (!TextUtils.isEmpty(supplierEmail)) {
                btSendEmail.setVisibility(View.VISIBLE);
            } else {
                btSendEmail.setVisibility(View.GONE);
            }

            // Load product item
            loadProductItem();


            // Load order details for pdf
            pdfOrdNum = cursor.getString(cursor.getColumnIndexOrThrow(OrdersContract.OrderEntry._ID));
            pdfLoadProductItems();


        }
    }

    private void loadProductItem() {

        // Load order products items
        if (orderProductCursor!= null && orderProductCursor.moveToFirst()){
            int newProductId = productItemId - orderProductCursor.getCount() * PRODUCT_ITEM_ADD;

            for (int pos = 1; pos <= orderProductCursor.getCount(); pos++) {
                newProductId = newProductId + PRODUCT_ITEM_ADD;

                int productId = newProductId + PRODUCT_ID;
                int productIdInt = orderProductCursor.getInt(orderProductCursor.getColumnIndexOrThrow(OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_ID));

                TextView currentProductIdTv = (TextView) getActivity().findViewById(productId);
                currentProductIdTv.setText(String.valueOf(productIdInt));

                int spinnerId = newProductId + PRODUCT_SPINNER_IDS;
                Spinner spinnerProduct = (Spinner) getActivity().findViewById(+spinnerId);

                for (int pos2 = productList.getCount(); pos2 >= 0; pos2--) {
                    if (spinnerProductAdapter.getItemId(pos2) == productIdInt) {
                        spinnerProduct.setSelection(pos2);

                        break;
                    }
                }

                int currentPriceId = newProductId + PRODUCT_PRICE_IDS;
                long currentPriceItemInt = orderProductCursor.getLong(orderProductCursor.getColumnIndexOrThrow(OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_PRICE));
                Double currentPriceItemDouble = currentPriceItemInt / 100.00;
                String currentProductPriceFormatted = currencyFormatter.format(currentPriceItemDouble);

                TextView currentPriceTv = (TextView) getActivity().findViewById(+currentPriceId);
                currentPriceTv.setText(currentProductPriceFormatted);

                int currentQuantityId = newProductId + PRODUCT_QUANTITY_IDS;
                long currentQuantityInt = orderProductCursor.getLong(orderProductCursor.getColumnIndexOrThrow(OrdersContract.OrderProductsEntry.COLUMN_ORDERPRODUCTS_PRODUCT_QUANTITY));

                EditText currentQuantityEt = (EditText) getActivity().findViewById(+currentQuantityId);
                currentQuantityEt.setText(String.valueOf(currentQuantityInt));

                int currentTotalId = newProductId + PRODUCT_TOTALS_IDS;
                long currentTotalInt = currentPriceItemInt * currentQuantityInt;
                Double currentTotalDouble = currentTotalInt / 100.00;
                String currentProductPriceTotalFormatted = currencyFormatter.format(currentTotalDouble);
                TextView currentTotalTv = (TextView) getActivity().findViewById(+currentTotalId);
                currentTotalTv.setText(currentProductPriceTotalFormatted);

                int currentProductOrderId = newProductId + PRODUCT_ORDER_ID;
                TextView currentProductOrderTv = (TextView) getActivity().findViewById(+currentProductOrderId);
                Long currentProductOrderLong = orderProductCursor.getLong(orderProductCursor.getColumnIndexOrThrow(OrdersContract.OrderProductsEntry._ID));
                currentProductOrderTv.setText(String.valueOf(currentProductOrderLong));

                orderProductCursor.moveToNext();
            }
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void replicateOrder() {
        replicateOrderSelector = true;
        currentOrderUri = null;
        orderProductId = null;
        saveOrder();
    }

    public interface OnCreateNewProductSelectedListener {
        void onNewProductSelected(String selectedSupplierNameAndId, Long currentOrderId);
    }





    @SuppressWarnings("deprecation")
    private void pdfCreateProductLines(){
        pdfProductItemId = pdfProductItemId + PRODUCT_ITEM_ADD;
        TableRow itemLine = new TableRow(getActivity());
        if (pdfProductItemId / PRODUCT_ITEM_ADD % 2 == 1) {
            itemLine.setBackgroundResource(R.color.item2);
        } else {
            itemLine.setBackgroundResource(R.color.item1);
        }
        pdfTable.addView(itemLine);

        TextView productTv = new TextView(getActivity());
        productTv.setId(pdfProductItemId + PRODUCT_SPINNER_IDS);
        itemLine.addView(productTv);
        productTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_pdf));
        productTv.setTextColor(getResources().getColor(R.color.main_text));


        TextView quantityTv = new TextView(getActivity());
        quantityTv.setGravity(Gravity.END);
        quantityTv.setId(pdfProductItemId + PRODUCT_QUANTITY_IDS);
        itemLine.addView(quantityTv);
        quantityTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_pdf));
        quantityTv.setTextColor(getResources().getColor(R.color.main_text));

        TextView priceTv = new TextView(getActivity());
        priceTv.setGravity(Gravity.END);
        priceTv.setId(pdfProductItemId + PRODUCT_PRICE_IDS);
        itemLine.addView(priceTv);
        priceTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_pdf));
        priceTv.setTextColor(getResources().getColor(R.color.main_text));

        TextView totalItemTv = new TextView(getActivity());
        totalItemTv.setGravity(Gravity.END);
        totalItemTv.setId(pdfProductItemId + PRODUCT_TOTALS_IDS);
        itemLine.addView(totalItemTv);
        totalItemTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_pdf));
        totalItemTv.setTextColor(getResources().getColor(R.color.main_text));
    }

    @SuppressWarnings("deprecation")
    private void pdfCreateTotalLine(){
        TableRow itemLine = new TableRow(getActivity());
        pdfTable.addView(itemLine);

        TextView totalTv = new TextView(getActivity());
        totalTv.setText(getString(R.string.total));
        itemLine.addView(totalTv);
        totalTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_pdf));
        totalTv.setTextColor(getResources().getColor(R.color.main_text));

        TextView quantityTotalTv = new TextView(getActivity());
        quantityTotalTv.setGravity(Gravity.END);
        itemLine.addView(quantityTotalTv);
        quantityTotalTv.setId(+PDF_TOTAL_QUANTITY_ID);
        quantityTotalTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_pdf));
        quantityTotalTv.setTextColor(getResources().getColor(R.color.main_text));

        TextView empty = new TextView(getActivity());
        itemLine.addView(empty);

        TextView totalValueTv = new TextView(getActivity());
        totalValueTv.setGravity(Gravity.END);
        totalValueTv.setId(+PDF_TOTAL_PRICE_ID);
        itemLine.addView(totalValueTv);
        totalValueTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_for_pdf));
        totalValueTv.setTextColor(getResources().getColor(R.color.main_text));

    }

    private void pdfLoadProductItems() {

        pdfOrderNumberTv.setText(pdfOrdNum);

        int lengthSupplierName = pdfSupName.length();

        if (lengthSupplierName > 20) {
            pdfSupplierNameTv.setText(pdfSupName.substring(0, 20));
        }
        else {
            pdfSupplierNameTv.setText(pdfSupName);
        }


        String orderDateEt = orderDateEditText.getText().toString();
        pdfOrderDateTv.setText(orderDateEt);

        if (deliveryDateEditText != null) {
            String deliveryDateEt = deliveryDateEditText.getText().toString();
            pdfDeliveryDateTv.setText(deliveryDateEt);
        }
        if (paymentDateEditText != null) {
            String paymentDateEt = paymentDateEditText.getText().toString();
            pdfPaymentDateTv.setText(paymentDateEt);
        }

        if (commentsEditText != null){
            String commentEt = commentsEditText.getText().toString();
            pdfComments.setText(commentEt);
        }


        if (totalQuantityEditText != null) {

            TextView totalQuantity = (TextView) getActivity().findViewById(+PDF_TOTAL_QUANTITY_ID);
            totalQuantity.setText(totalQuantityEditText.getText().toString());
        }

        if (totalPriceEditText != null) {
            TextView totalPrice = (TextView) getActivity().findViewById(+PDF_TOTAL_PRICE_ID);
            totalPrice.setText(totalPriceEditText.getText().toString());
        }


        // Load order products items
        orderProductCursor.moveToFirst();
        int newProductId = productItemId - orderProductCursor.getCount() * PRODUCT_ITEM_ADD;
        int newPdfId = 1000;
        for (int pos = 1; pos <= orderProductCursor.getCount(); pos++) {

            newProductId = newProductId + PRODUCT_ITEM_ADD;
            Spinner productSpinner = (Spinner) getActivity().findViewById(newProductId + PRODUCT_SPINNER_IDS);
            String productString = ((Cursor)productSpinner.getSelectedItem()).getString(
                    productList.getColumnIndexOrThrow(OrdersContract.ProductEntry.COLUMN_PRODUCT_NAME));

            int currentPriceId = newProductId + PRODUCT_PRICE_IDS;
            TextView currentPriceTv = (TextView) getActivity().findViewById(+currentPriceId);
            String currentPriceString = currentPriceTv.getText().toString();

            int currentQuantityId = newProductId + PRODUCT_QUANTITY_IDS;
            EditText currentQuantityEt = (EditText) getActivity().findViewById(+currentQuantityId);
            String currentQuantityString = numberFormatter.format((Long.parseLong(currentQuantityEt.getText().toString())));

            int currentTotalId = newProductId + PRODUCT_TOTALS_IDS;
            TextView currentTotalTv = (TextView) getActivity().findViewById(+currentTotalId);
            String currentTotalString = currentTotalTv.getText().toString();

            // Set to the view
            newPdfId = newPdfId + PRODUCT_ITEM_ADD;
            TextView pdfProductName = (TextView) getActivity().findViewById(newPdfId + PRODUCT_SPINNER_IDS);
            pdfProductName.setText(productString);

            int pdfCurrentPriceId = newPdfId + PRODUCT_PRICE_IDS;
            TextView pdfCurrentPriceTv = (TextView) getActivity().findViewById(+pdfCurrentPriceId);
            pdfCurrentPriceTv.setText(currentPriceString);

            int pdfCurrentQuantityId = newPdfId + PRODUCT_QUANTITY_IDS;
            TextView pdfCurrentQuantityTv = (TextView) getActivity().findViewById(+pdfCurrentQuantityId);
            pdfCurrentQuantityTv.setText(currentQuantityString);

            int pdfCurrentTotalId = newPdfId + PRODUCT_TOTALS_IDS;
            TextView pdfCurrentTotalTv = (TextView) getActivity().findViewById(+pdfCurrentTotalId);
            pdfCurrentTotalTv.setText(currentTotalString);

            orderProductCursor.moveToNext();
        }


    }

}
