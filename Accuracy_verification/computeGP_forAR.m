clearvars
close all
clc

%% Initialize variables
EO_all=load('EO_opk_test1.txt');

% Visualize GP
gp = load('GP.txt');
plot3(gp(:,2), gp(:,3), gp(:,4), 'r^','LineWidth',2);
view(3)
grid on, axis equal
xlabel('X'), ylabel('Y'), zlabel('Z')
% 
% % Visualize the CS from Photoscan
% hold on;
% for i = 1:size(EO_all)
%     ori = pi / 180 * [EO_all(i, 5) EO_all(i, 6) EO_all(i, 7)];
%     R = Rot3D(ori);
%     vis_coord_system(EO_all(i, 2:4)', R, 5, '', 'r');
% end

p{1} = [205154.2278	553721.761 77.55746]';      % 25
p{2} = [205154.0753	553719.4836	77.56137]';     % 29
p{3} = [205154.2146	553721.7655	79.92669]';     % 1

% Define the normal vector of the plane
v1 = p{2} - p{1};       % x-axis
v2 = p{3} - p{1};       % pseudo y-axis
nv = cross(v1, v2);     % normal vector: z-axis
d = dot(nv, p{1});

% Coordinates in CCS
pixel_size = 0.001419771e-3;    % m/pix
focal_length = 4.4928763627;    % mm
ccs = load('IP.txt');

%% Process
NoGP = size(ccs,1);
IP2GP = zeros(NoGP, 5);

azimuth = -10.532 * pi / 180;
% R matrix Local -> Camera
%Rcl = [1 0 -0.02; 0 1 0.03; 0.02 -0.03 1];
Rcl = [1 0 0; 0 1 0; 0 0 1];
% R matrix World -> Local
wl_params = [0, 0, -azimuth];
Rwl = Rot3D(wl_params);
R_test = Rwl;

hold on;
vis_coord_system(EO_all(1, 2:4)', R_test, 5, '', 'b');

Rr = [0.05 0.66 0.75; 1	0.03 0.04; 0.01	0.75 -0.66];
vis_coord_system(EO_all(1, 2:4)', R_test*Rr, 5, '', 'g');

for i = 1:NoGP
    imgIdx = find(ccs(i,1)==EO_all(:,1));
    EO=EO_all(imgIdx, 2:7);
    
    % Rotation Matrix
    ori = pi / 180 * [EO(4) EO(5) EO(6)];
    R = Rot3D(ori);
    
    % Distortion correction
    
    % Compute GPs
    coordCCS = [ccs(i, 3:4) -focal_length];     % unit: m
    proj_coord = xy_g_min(EO, R, coordCCS', nv, d);    % compute the ground coordinates
    IP2GP(i,:) = [ccs(i,1) ccs(i,2) proj_coord'];
end



