// meris L1 transekte
// RD 20140219

// (1) set path, constants and activate the neural network programs
exex=exists('execnn');
if  exex == 0 then
    cd 'F:\backup_notebook_d_20130823\projekte\validation_20130930\heglo_transect\breadboard_20150204';
    // MERIS bands
    merband12=[412.3, 442.3, 489.7, 509.6, 559.5, 619.4, 664.3, 680.6, 708.1, 753.1, 778.2, 864.6];
    merband10 =[412.3, 442.3, 489.7, 509.6, 559.5, 619.4, 664.3, 680.6, 708.1, 753.1];
    merband12_ix=[1,2,3,4,5,6,7,8,9,10,12,13];

    // gas absorption constants
    absorb_ozon=[8.2e-04, 2.82e-03, 2.076e-02, 3.96e-02, 1.022e-01, 1.059e-01, 5.313e-02, 3.552e-02, 1.895e-02, 8.38e-03, 7.2e-04, 0.0];
    h2o_cor_poly=[0.3832989, 1.6527957, -1.5635101, 0.5311913]; // polynom coefficients for band708 H2O correction

    deg2rad=%pi/180.0;
    rad2deg=180.0/%pi;

    // activate NN programs
    exec('./nnhs.sce');
    exec('./nnhs_ff.sce');
end
execnn=123;

// (2) activate the neural networks

// rtosa auto NN
aa_rtosa_nn_bn7_9 = nnhs('../nets\richard_atmo_invers29_press_20150125\rtoa_aaNN7\31x7x31_555.6.net');

// rtosa-rw NN
inv_ac_nn9=nnhs('../nets\richard_atmo_invers29_press_20150125\rtoa_rw_nn3\33x73x53x33_470639.6.net');

// rtosa - rpath NN
rpath_nn9 = nnhs('../nets\richard_atmo_invers29_press_20150125\rtoa_rpath_nn2\31x77x57x37_2388.6.net');

// rtosa - trans NN
inv_trans_nn = nnhs('..\nets\richard_atmo_invers29_press_20150125\rtoa_trans_nn2\31x77x57x37_37087.4.net');

// rw-IOP inverse NN
inv_nn7=nnhs('../nets\coastcolour_wat_20140318\inv_meris_logrw_logiop_20140318_noise_p5_fl\97x77x37_11671.0.net'); //chl reduction by 

// IOP-rw forward NN
for_nn9b=nnhs('../nets\coastcolour_wat_20140318\for_meris_logrw_logiop_20140318_p5_fl\17x97x47_335.3.net'); //only 10 MERIS bands

// rw-kd NN, output are kdmin and kd449
kd2_nn7 = nnhs('../nets\coastcolour_wat_20140318\inv_meris_kd\97x77x7_232.4.net');

// uncertainty NN for IOPs after bias corretion
unc_biasc_nn1=nnhs('..\nets\coastcolour_wat_20140318\uncertain_log_abs_biasc_iop\17x77x37_11486.7.net');

// uncertainty for atot, adg, btot and kd
unc_biasc_atotkd_nn=nnhs('..\nets\coastcolour_wat_20140318\uncertain_log_abs_tot_kd\17x77x37_9113.1.net');


// (3) MERIS input files
l1b_file='MER_RR__1PTACR20051013_095419_000001802041_00337_18928_0000_radiance_5_c25_20051013_trios_latlon_Mask.txt';
mersol_file = 'c25_20051013_solflux.txt'; //solar flux
out_file = 'MER_RR__1PTACR20051013_outfile.txt';

// (4)define array variables, only for plotting
longi_a=[];
lati_a=[];
transd_a_nn=[];
transu_a_nn=[];
rw_a_nn=[];
iop_nn1=[];
kdmin_nn=[];
kd489_nn=[];
ap_a_nn1=[];
ad_a_nn1=[];
ag_a_nn1=[];
bp_a_nn1=[];
bw_a_nn1=[];
adg_a_nn1=[];
atot_a_nn1=[];
btot_a_nn1=[];
chl_a_nn1=[];
tsm_a_nn1=[];
unc_iop_abs=[];
unc_iop_rel=[];
unc_iop_abs_plus=[];
unc_iop_abs_min=[];
unc_abs_chl=[];
unc_abs_tsm=[];
unc_abs_adg=[];
unc_abs_atot=[];
unc_abs_btot=[];
unc_abs_kd489=[];
unc_abs_kdmin=[];
unc_iop_abs_lin=[];
flag_rw=[];
flag_rtosa=[];
tosa_oor_flag=[];
rw_oor_flag=[];
s1_test_a=[];
rtosa_aaNNrat_minmax_a=[];

rtosa_aaNNrat_a=[];
diff_rw=[];
diff_rw_sum=[];

// (5) thresholds for flags
thresh_rtosaaaNNrat=[0.95 1.05];  // threshold for out of scope flag Rtosa has to be adjusted
thresh_rwslope=[0.95 1.05];    // threshold for out of scope flag Rw has to be adjusted

// (6) set temperature and salinity mean value, replace by actual value if available
temperature=15.0;
salinity=35.0;

// (7) open and read solflux data
fp_sol=mopen(mersol_file);
dum=mgetl(fp_sol,1); // jump over headerline
for i=1:15,
    [n mer_lam(i), solflux(i)] = mfscanf(fp_sol,'%f%f\n');
end
mclose(fp_sol);


// (8) open MERIS L1b transect and output file
fp_mer=mopen(l1b_file);
dum=mgetl(fp_mer,1); // jump over header line of MERIS data

// open output file
fp_out=mopen(out_file,'w');


// (9) loop over all MERIS transect data
ipix=1;
while ~meof(fp_mer) do
    // (9.1) read 1 line
    [n, pixx, pixy, longi, lati]=mfscanf(fp_mer,'%f %f %f %f');
    toa_rad=mfscanf(15,fp_mer,'%f');
    [n, l1flag, detector_ix, latitude, longitude, dem_alt, dem_rough, lat_corr, lon_corr, sun_zeni, sun_azi, view_zeni, view_azi, zon_wind, merid_wind, atm_press, ozone, rel_hum]=mfscanf(fp_mer,'%f%f%f%f%f%f%f%f%f%f%f%f%f%f%f%f%f\n');

    longi_a(ipix)=longi;
    lati_a(ipix) =lati;

    //  (9.2) compute angles 
    cos_sun=cos(sun_zeni*deg2rad);
    cos_view=cos(view_zeni*deg2rad);
    sin_sun=sin(sun_zeni*deg2rad);
    sin_view=sin(view_zeni*deg2rad);

    cos_azi_diff=cos((view_azi-sun_azi)*deg2rad);
    azi_diff_rad=acos(cos_azi_diff); 
    sin_azi_diff=sin(azi_diff_rad);
    azi_diff_deg=azi_diff_rad*rad2deg;

    x=sin_view*cos_azi_diff;
    y=sin_view*sin_azi_diff;
    z=cos_view;

    r_toa = toa_rad./(solflux.*cos_sun)*%pi;
    r_tosa_ur=r_toa(merband12_ix);

    // (9.3.0) +++ water vapour correction for band 9 +++++ */
    //X2=rho_900/rho_885;
    X2=r_toa(15)/r_toa(14);
    trans708=h2o_cor_poly(1)+h2o_cor_poly(2)*X2+h2o_cor_poly(3)*X2*X2+h2o_cor_poly(4)*X2*X2*X2;
    r_tosa_ur(9) = r_tosa_ur(9)/ trans708;

    //*** (9.3.1) ozone correction ***/
    model_ozone=0;

    trans_ozoned12=exp(-(absorb_ozon.*ozone./1000-model_ozone)./cos_sun);
    trans_ozoneu12=exp(-(absorb_ozon.*ozone./1000-model_ozone)./cos_view);
    trans_ozone12=trans_ozoned12 .*trans_ozoneu12;

    r_tosa_oz=r_tosa_ur./trans_ozone12';

    r_tosa=r_tosa_oz';
    log_rtosa=log(r_tosa);


    // (9.3.2) altitude pressure correction
    // this is only a very simplified formula, later use more exact one
    // also for larger lakes the dem_alt presently provideds the altitude of the lake bottom
    // will be changed later to altitude of the lake surface
    if dem_alt > 10.0 then
        alti_press=atm_press*exp(-dem_alt/8000); 
    else
        alti_press=atm_press;
    end

    // (9.4) )set input to all atmosphere NNs   
    nn_in=[sun_zeni,x,y,z,temperature, salinity, alti_press, log_rtosa];

    // (9.5) test out of scope spectra with autoassociative neural network
    log_rtosa_aann=nnhs_ff(aa_rtosa_nn_bn7_9,nn_in);
    rtosa_aann=exp(log_rtosa_aann);
    rtosa_aaNNrat= (rtosa_aann./r_tosa);
    rtosa_aaNNrat_a(ipix,:)=rtosa_aaNNrat;

    // (9.6.1) set rho_toa out of scope flag
    rtosa_aaNNrat_min=min(rtosa_aaNNrat);
    rtosa_aaNNrat_max=max(rtosa_aaNNrat);
    rtosa_aaNNrat_minmax_a(ipix) = max([rtosa_aaNNrat_max (1.0./rtosa_aaNNrat_min)]);

    flag_rtosa(ipix)=0;
    if rtosa_aaNNrat_min < thresh_rtosaaaNNrat(1) | rtosa_aaNNrat_max > thresh_rtosaaaNNrat(2) then
        flag_rtosa(ipix)=1; // set flag if difference of band 5 > threshold       
    end

    // (9.6.2) test if input tosa spectrum is out of range
    mima=aa_rtosa_nn_bn7_9(5); // minima and maxima of aaNN input
    tosa_oor_flag(ipix)=0;
    for iv=1:19,// variables
        if nn_in(iv)< mima(iv,1)| nn_in(iv)> mima(iv,2)
            tosa_oor_flag(ipix)=1;         
        end
    end
    // (9.7) NN compute rpath from rtosa
    log_rpath_nn=nnhs_ff(rpath_nn9,nn_in);
    rpath_nn=exp(log_rpath_nn);
    rpath_a_nn(ipix,:)=rpath_nn;

    // (9.8) NN compute transmittance from rtosa
    trans_nn=nnhs_ff(inv_trans_nn,nn_in);
    transd_a_nn(ipix,:)=trans_nn(1:12);
    transu_a_nn(ipix,:)=trans_nn(13:24);

    // (9.9) NNcompute rw from rtosa
    log_rw=nnhs_ff(inv_ac_nn9,nn_in);
    rw=exp(log_rw);
    rw_a_nn(ipix,:)=rw;

    // (9.10.1) NN compute IOPs from rw

    // define input to water NNs
    nn_in_inv=[sun_zeni view_zeni azi_diff_deg temperature salinity log_rw(1:10)];
    log_iops_nn1=nnhs_ff(inv_nn7,nn_in_inv);
    iops_nn1=exp(log_iops_nn1);
    iop_nn1(ipix,:)=iops_nn1;

    // (9.10.2) test if input tosa spectrum is out of range
    mima=inv_nn7(5); // minima and maxima of aaNN input
    rw_oor_flag(ipix)=0;
    for iv=1:15,// variables
        if nn_in_inv(iv)< mima(iv,1)| nn_in_inv(iv)> mima(iv,2)
            rw_oor_flag(ipix)=1;
        end
    end

    // (9.11) test out of scope of rho_w by combining inverse and forward NN

    // define input to forward water NN
    nn_in_for=[sun_zeni view_zeni azi_diff_deg temperature salinity log_iops_nn1];

    // compute rho_w from IOPs
    log_rw_nn2 = nnhs_ff(for_nn9b,nn_in_for);

    if modulo(ipix,1000)== 0 then // plot of rw spectra and compare with rw_forNN, if modulo has a low value
        plot(merband10,rw(1:10),merband10,exp(log_rw_nn2));
        hed=msprintf('ipix= %3d longi %5.2f',ipix,longi);
        xtitle(hed,'wavelength','rw');
        legend(['rw_in' 'rw_out']);
        pause
        xdel(0);
    end

    // (9.12) compute the test and set rw is out of scope flag
    s1_mess=log_rw(5)-log_rw(2);
    s2_mess=log_rw(6)-log_rw(5);
    s1_nn2=log_rw_nn2(5)-log_rw_nn2(2);
    s2_nn2=log_rw_nn2(6)-log_rw_nn2(5);
    s1_test=exp(s1_nn2)./exp(s1_mess);
    if s1_test < thresh_rwslope(1) | s1_test > thresh_rwslope(2) then
        flag_rw(ipix)=1
    else
        flag_rw(ipix)=0;
    end    
    s1_test_a(ipix)=(s1_test);

    // (9.13) NN compute kd from rw
    log_kd2_nn=nnhs_ff(kd2_nn7,nn_in_inv);
    kdmin_nn(ipix)=exp(log_kd2_nn(1));
    kd489_nn(ipix)=exp(log_kd2_nn(2));

    // (9.14) compute combined IOPs and concentrations
    // split IOPs
    log_conc_ap_nn1=log_iops_nn1(1);
    log_conc_ad_nn1=log_iops_nn1(2);
    log_conc_ag_nn1=log_iops_nn1(3);
    log_conc_bp_nn1=log_iops_nn1(4);
    log_conc_bw_nn1=log_iops_nn1(5);

    ap_a_nn1(ipix)=exp(log_conc_ap_nn1);
    ad_a_nn1(ipix)=exp(log_conc_ad_nn1);
    ag_a_nn1(ipix)=exp(log_conc_ag_nn1);
    bp_a_nn1(ipix)=exp(log_conc_bp_nn1);
    bw_a_nn1(ipix)=exp(log_conc_bw_nn1);

    // combine IOPs
    adg_a_nn1(ipix)= ad_a_nn1(ipix)+ag_a_nn1(ipix);
    atot_a_nn1(ipix)=adg_a_nn1(ipix)+ap_a_nn1(ipix);
    btot_a_nn1(ipix)= bp_a_nn1(ipix)+bw_a_nn1(ipix);

    // compute concentrations
    chl_a_nn1(ipix)=21.0.*(ap_a_nn1(ipix))^(1.04);
    tsm_a_nn1(ipix) = btot_a_nn1(ipix)*1.73;

    // (9.15) )NN compute uncertainties
    diff_log_abs_iop=nnhs_ff(unc_biasc_nn1,log_iops_nn1);
    diff_log_abs_iop_a(ipix,:)=diff_log_abs_iop;
    unc_iop_rel(ipix,:)=(exp(diff_log_abs_iop)-1).*100;
    unc_iop_abs(ipix,:)=iop_nn1(ipix,:).*(1.0-exp(-diff_log_abs_iop));

    unc_abs_chl(ipix) = 21.0.*unc_iop_abs(ipix,1).^(1.04);

    // (9.16) NN compute uncertainties for combined IOPs and kd
    diff_log_abs_combi_kd=nnhs_ff(unc_biasc_atotkd_nn,log_iops_nn1);
    diff_log_abs_adg  = diff_log_abs_combi_kd(1);
    diff_log_abs_atot = diff_log_abs_combi_kd(2);
    diff_log_abs_btot = diff_log_abs_combi_kd(3);
    diff_log_abs_kd489= diff_log_abs_combi_kd(4);
    diff_log_abs_kdmin= diff_log_abs_combi_kd(5);

    unc_abs_adg(ipix)   = (1.0-exp(-diff_log_abs_adg)).* adg_a_nn1(ipix);
    unc_abs_atot(ipix)  = (1.0-exp(-diff_log_abs_atot)).* atot_a_nn1(ipix);
    unc_abs_btot(ipix)  = (1.0-exp(-diff_log_abs_btot)).* btot_a_nn1(ipix);
    unc_abs_kd489(ipix) = (1.0-exp(-diff_log_abs_kd489)).*kd489_nn(ipix);
    unc_abs_kdmin(ipix) = (1.0-exp(-diff_log_abs_kdmin)).*kdmin_nn(ipix);
    unc_abs_tsm(ipix) = 1.73.*unc_abs_btot(ipix); 

    // (9.17) output results
    ut1=[latitude longitude sun_zeni view_zeni azi_diff_deg zon_wind merid_wind atm_press ozone rel_hum temperature salinity];
    mfprintf(fp_out,'%10.3e',ut1');
    mfprintf(fp_out,'%10.3e',rw');  
    mfprintf(fp_out,'%10.3e',rpath_nn');  
    mfprintf(fp_out,'%10.3e',trans_nn');  // 12 transd and 12 transu
    mfprintf(fp_out,'%10.3e',iops_nn1');
    mfprintf(fp_out,'%10.3e',unc_iop_abs(ipix,:)');
    ut2=[chl_a_nn1(ipix) tsm_a_nn1(ipix) kdmin_nn(ipix) kd489_nn(ipix)};
    mfprintf(fp_out,'%10.3e',ut2');    
    ut3=[unc_abs_chl(ipix) unc_abs_adg(ipix) unc_abs_atot(ipix) unc_abs_btot(ipix) unc_abs_kdmin(ipix) unc_abs_tsm(ipix)]; 
    mfprintf(fp_out,'%10.3e',ut3');
    mfprintf(fp_out,'%10.3e%10.3e',rtosa_aaNNrat,s1_test);
    mfprintf(fp_out,' %1d%1d%1d%1d', int(flag_rtosa(ipix)), int(tosa_oor_flag(ipix)), int(flag_rw(ipix)), int(rw_oor_flag(ipix)));
    mfprintf(fp_out,'\n');

    ipix=ipix+1;
end; // end loop over all pixels of the transect
mclose(fp_mer);
mclose(fp_out);

// (9.18) plot results
ib=5;
hed='MER_RR__1PTACR20051013';
hedib=msprintf('%s rho_w band %2d',hed,ib);
scf();
plot(longi_a,rw_a_nn(:,ib),'+');
xtitle(hedib,'longitude','rho_w');


hedib=msprintf('%s rho_path band %2d',hed,ib);
scf();
plot(longi_a,rpath_a_nn(:,ib),'+');
xtitle(hedib,'longitude','rho_path');

hedib=msprintf('%s transd band %2d',hed,ib);
scf();
plot(longi_a,transd_a_nn(:,ib),'+');
xtitle(hedib,'longitude','trans_down');

hedib=msprintf('%s transu band %2d',hed,ib);
scf();
plot(longi_a,transu_a_nn(:,ib),'+');
xtitle(hedib,'longitude','trans_down');

hedib=msprintf('%s aaNN aaNNrat_minmax',hed);
scf();
plot2d(longi_a,rtosa_aaNNrat_minmax_a,style=[ -1],rect=[min(longi_a) 1.0 max(longi_a) 1.2]);
//ly=[0.95 0.95];
hy=[1.05 1.05];
my=[1.0 1.0];
xx=[min(longi_a) max(longi_a)];
plot2d(xx',[hy'],style=[ 5]);
xtitle(hedib,'longitude','aaNN_aaNNrat');

hedib=msprintf('%s s1 test',hed);
scf();
plot2d(longi_a,s1_test_a,style = -1,rect=[min(longi_a) 0.9 max(longi_a) 1.1]);
ly=[0.95 0.95];
hy=[1.05 1.05];
my=[1.0 1.0];
xx=[min(longi_a) max(longi_a)];
plot2d(xx',[ly' hy' my'],style=[ 5 5 3]);
xtitle(hedib,'longitude','s1 test');

hedib=msprintf('%s chl.',hed);
scf();
plot(longi_a,chl_a_nn1,'+',longi_a,unc_abs_chl,'o');
xtitle(hedib,'longitude','chl / unc [mg m-3]');
legend(['chl' 'uncertainty'],2);

unc_iop_abs_lin
hedib=msprintf('%s TSM',hed);
scf();
plot(longi_a,tsm_a_nn1,'+',longi_a,unc_abs_tsm,'o');
xtitle(hedib,'longitude','TSM / unc [g m-3]');
legend(['TSM' 'uncertainty'],2);

hedib=msprintf('%s adg 443',hed);
scf();
plot(longi_a,adg_a_nn1,'+',longi_a,unc_abs_adg,'o');
xtitle(hedib,'longitude','adg / unc [m-1]');
legend(['adg' 'uncertainty'],2);

hedib=msprintf('%s kd',hed);
scf();
plot(longi_a,kdmin_nn,'+',longi_a,unc_abs_kdmin,'o');
xtitle(hedib,'longitude','kdmin / unc [m-1]');
legend(['kdmin' 'uncertainty'],2);

