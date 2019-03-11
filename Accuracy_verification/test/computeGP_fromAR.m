clearvars
close all
clc

%% Initialize variables
EO_all=load('Jinwoo_EO_OPK_test.txt');

% Visualize GP
gp = load('GP.txt');
plot3(gp(:,2), gp(:,3), gp(:,4), 'r^','LineWidth',2);
view(3)
grid on, axis equal
xlabel('X'), ylabel('Y'), zlabel('Z')

% Visualize the CS from Photoscan
hold on;
ori = pi / 180 * [EO_all(1, 5) EO_all(1, 6) EO_all(1, 7)];
R = Rot3D(ori);     % Ground -> Camera
vis_coord_system(EO_all(1, 2:4)', R', 5, '', 'r');

% Compute the azimuth from processed data
rot = R';
test_vec = -rot(:,1);   % -X axis
test_y = [0 1 0]';      % North direction

azimuth_dot = acos(dot(test_y, test_vec) / (norm(test_y) * norm(test_vec)));
azimuth_dot = azimuth_dot * 180 / pi;

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
ccs = load('IP_AR_test.txt');

%% Process
NoGP = size(ccs,1);
IP2GP = zeros(NoGP, 5);

% azimuth = (-1.798) * pi / 180;
% azimuth = -azimuth;
azimuth = azimuth_dot * pi / 180;

% R matrix Local -> Camera
x = [-0.08 1 -0.02]';
y = [-1 -0.08 0.01]';
z = [0.01 0.02 1]';
Rcl = [x y z];
Rcl = [x/norm(x) y/norm(y) z/norm(z)];
Rlc = Rcl';

% R matrix Ground -> Local
% gl_params = [0, 0, azimuth];
gl_params = [pi/2, -(pi/2-azimuth), 0];
Rgl = Rot3D(gl_params);

% Ground -> Camera
R_test = Rlc*Rgl;

hold on;
vis_coord_system(EO_all(1, 2:4)', R_test', 5, '', 'b');

for i = 1:NoGP
    imgIdx = find(ccs(i,1)==EO_all(:,1));
    EO=EO_all(imgIdx, 2:7);
    
    % Rotation Matrix
    ori = pi / 180 * [EO(4) EO(5) EO(6)];
    R = Rot3D(ori);
    
    % Distortion correction
    
    % Compute GPs
    coordCCS = [ccs(i, 3:4) -focal_length];     % unit: mm
%     proj_coord = xy_g_min(EO, R, coordCCS', nv, d);    % compute the ground coordinates
    proj_coord = xy_g_min(EO, R_test, coordCCS', nv, d);    % compute the ground coordinates
    IP2GP(i,:) = [ccs(i,1) ccs(i,2) proj_coord'];
end




