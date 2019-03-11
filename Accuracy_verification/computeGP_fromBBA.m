clearvars
close all
clc

%% Initialize variables
EO_all=load('Jinwoo_EO_OPK_test.txt');

p{1} = [205154.2278	553721.761 77.55746]';      % 25
p{2} = [205154.0753	553719.4836	77.56137]';     % 29
p{3} = [205154.2146	553721.7655	79.92669]';     % 1

% Define the normal vector of the plane
v1 = p{2} - p{1};       % x-axis
v2 = p{3} - p{1};       % pseudo y-axis
nv = cross(v1, v2);     % normal vector: z-axis
d = dot(nv, p{1});

% Coordinates in CCS
pixel_size = 0.001419771;    % mm/pix
focal_length = 3137.53 * pixel_size;    % mm
ccs = load('IP_BBA_test.txt');

%% Process
NoGP = size(ccs,1);
IP2GP = zeros(NoGP, 5);

for i = 1:NoGP
    imgIdx = find(ccs(i,1)==EO_all(:,1));
    EO=EO_all(imgIdx, 2:7);
    
    % Rotation Matrix
    ori = pi / 180 * [EO(4) EO(5) EO(6)];
    R = Rot3D(ori);
    
    % Distortion correction
    
    % Compute GPs
    coordCCS = [ccs(i, 3:4) -focal_length];     % unit: mm
    proj_coord = xy_g_min(EO, R, coordCCS', nv, d);    % compute the ground coordinates
    IP2GP(i,:) = [ccs(i,1) ccs(i,2) proj_coord'];
end




