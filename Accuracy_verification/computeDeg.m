clearvars
close all
clc

%% Initialize variables
data_BA = './01_BA/';
data_AR = './02_AR/';
data_SPR = './03_SPR/';

% Exterior Orientation Parameters
EO_BA_all = load(strcat(data_BA, 'EO_opk.txt'));
% EO_AR_all = load(strcat(data_AR, 'EO_azimuth.txt'));
EO_AR_all = load(strcat(data_SPR, 'EO_azimuth_SPR2.txt'));

%% Check the Coordinate System
% Visualize GP
gp = load('GP2_20190522.txt');
plot3(gp(:,2), gp(:,3), gp(:,4), 'r^','LineWidth',2);
view(3)
grid on, axis equal
xlabel('X'), ylabel('Y'), zlabel('Z')

%% Visualize BA coordinate system
for i = 1:size(EO_BA_all, 1)
    EO_BA = EO_BA_all(i, 2:7);
    
    % Rotation Matrix
    ori = pi / 180 * [EO_BA(4) EO_BA(5) EO_BA(6)];
    R = Rot3D(ori);
    
    hold on;
    idx = num2str(i);
    vis_coord_system(EO_BA(1:3)', R', 5, idx, 'b');
    
    EO_AR = EO_AR_all(i, 2:end);
    
    % Rotation Matrix
    % 1) Rotation matrix Ground -> Local
    azimuth = EO_AR(4) * pi / 180;
    azimuth = -azimuth;    
    gl_params = [pi/2, azimuth, 0];
    Rgl = Rot3D(gl_params);
    
    % 2) Rotation matrix Local -> Camera
    x = EO_AR(5:7)';
    y = EO_AR(8:10)';
    z = EO_AR(11:13)';
    Rcl = [x y z];
%     Rcl = [x/norm(x) y/norm(y) z/norm(z)];
    Rlc = Rcl';

    % 3) Rotation matrix Ground -> Camera
    R_AR = Rlc*Rgl;     

    hold on;
    idx = num2str(i);
    vis_coord_system(EO_AR(1:3)', R_AR', 5, idx, 'g');    
    
    % Compute degress
    deg_x = acos(dot(R(1,:), R_AR(1, :))/(norm(R(1,:))*norm(R_AR(1, :))));
    deg_y = acos(dot(R(2,:), R_AR(2, :))/(norm(R(2,:))*norm(R_AR(2, :))));
    deg_z = acos(dot(R(3,:), R_AR(3, :))/(norm(R(3,:))*norm(R_AR(3, :))));
    deg_x = deg_x * 180 / pi;
    deg_y = deg_y * 180 / pi;
    deg_z = deg_z * 180 / pi;
    
    deg(i, :) = [deg_x deg_y deg_z];
end

% %% Visualize AR coordinate system
% for i = 1:size(EO_AR_all,1)
%     EO = EO_AR_all(i, 2:end);
%     
%     % Rotation Matrix
%     % 1) Rotation matrix Ground -> Local
%     azimuth = EO(4) * pi / 180;
%     azimuth = -azimuth;    
%     gl_params = [pi/2, azimuth, 0];
%     Rgl = Rot3D(gl_params);
%     
%     % 2) Rotation matrix Local -> Camera
%     x = EO(5:7)';
%     y = EO(8:10)';
%     z = EO(11:13)';
%     Rcl = [x y z];
% %     Rcl = [x/norm(x) y/norm(y) z/norm(z)];
%     Rlc = Rcl';
% 
%     % 3) Rotation matrix Ground -> Camera
%     R_AR = Rlc*Rgl;     
% 
%     hold on;
%     idx = num2str(i);
%     vis_coord_system(EO(1:3)', R_AR', 5, idx, 'g');    
%     
%     % Compute degress
% end




