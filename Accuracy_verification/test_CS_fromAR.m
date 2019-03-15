clearvars
close all
clc

%% Initialize variables
% Interior Orientation Parameters
pixel_size = 0.001419771;    % mm/pix
focal_length = 3137.53 * pixel_size;    % mm

% Normal Vector
p{1} = [205154.2278	553721.761 77.55746]';      % 25
p{2} = [205154.0753	553719.4836	77.56137]';     % 29
p{3} = [205154.2146	553721.7655	79.92669]';     % 1

v1 = p{2} - p{1};       % x-axis
v2 = p{3} - p{1};       % pseudo y-axis
nv = cross(v1, v2);     % normal vector: z-axis
d = dot(nv, p{1});

% Image Points
IP = load('IP_AR.txt');

% Exterior Orientation Parameters
EO_all=load('EO_opk_azimuth_R.txt');

%% Check the Coordinate System
% Visualize GP
gp = load('GP.txt');
plot3(gp(:,2), gp(:,3), gp(:,4), 'r^','LineWidth',2);
view(3)
grid on, axis equal
xlabel('X'), ylabel('Y'), zlabel('Z')


%% Process
NoIP = size(IP,1);
IP2GP = zeros(NoIP, 5);

for i = 1:1
%     imgIdx = find(IP(i,1)==EO_all(:,1));
    EO=EO_all(i, 2:end);
    
    % Rotation Matrix
    ori = pi / 180 * [EO(4) EO(5) EO(6)];
    R_GC = Rot3D(ori);
    
    hold on;
    idx = num2str(i);
    vis_coord_system(EO(1:3)', R_GC', 5, idx, 'r');   
        
    % Rotation Matrix
    % 1) Rotation matrix Ground -> Local
    azimuth = EO(7) * pi / 180;
    azimuth = -azimuth;    

    % 2) Rotation matrix Local -> Camera
    x = EO(8:10)';
    y = EO(11:13)';
    z = EO(14:16)';
    Rcl = [x/norm(x) y/norm(y) z/norm(z)];
    Rlc = Rcl';    
    
    if i == 1        
        gl_params = [pi/2, -(pi/2-azimuth), 0];
        Rgl = Rot3D(gl_params);
    elseif i == 2        
        gl_params = [pi/2, azimuth, 0];
        Rgl = Rot3D(gl_params);
    elseif i == 3        
        gl_params = [pi/2, -(pi/2-azimuth), 0];
        Rgl = Rot3D(gl_params);
    end
    
    % 3) Rotation matrix Ground -> Camera
    R = Rlc*Rgl;
%     R = Rgl;

    hold on;
    idx = num2str(i);
    vis_coord_system(EO(1:3)', R', 5, idx, 'b');
end




