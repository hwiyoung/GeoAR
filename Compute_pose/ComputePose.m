clc
clearvars

% GPS = [196538.591    548248.593    47.7105]';
% GPS(3) = GPS(3) + 1.7;
% Anchor = [196545.595    548238.6565    47.7685]';
% azimuth = 144.8209;
% % azimuth = 324.8209;

% % Rooftop -> tile
% GPS = [196538.591    548248.593    47.7105]';
% GPS(3) = GPS(3) + 1.7;
% Anchor = [196538.7984, 548250.1113, 48.035]';
% diff = Anchor - GPS;
% azimuth = atan(diff(1)/diff(2))*180/pi;

% 시립대 주차장
GPS = [204993.795, 553762.29, 62.884]'; % 14
GPS(3) = GPS(3) + 1.7;
Anchor = [204999.2523, 553762.2686, 62.9335]';
diff = Anchor - GPS;
% azimuth = atan(diff(1)/diff(2))*180/pi;
azimuth = atan(abs(diff(2))/abs(diff(1)))*180/pi + 90;

if azimuth >= 0
    theta = -(180 - azimuth) * pi / 180; 
else
    theta = (180 + azimuth) * pi / 180;
end

%% Translation in LCS
R_GL = Rot3D([pi/2, -azimuth*pi/180, 0]);
P_L = R_GL * (Anchor - GPS);

%% Euler Angles to Quaternion
yaw = 0; pitch = theta; roll = 0;

cy = cos(yaw * 0.5);
sy = sin(yaw * 0.5);
cp = cos(pitch * 0.5);
sp = sin(pitch * 0.5);
cr = cos(roll * 0.5);
sr = sin(roll * 0.5);

w2 = cy * cp * cr + sy * sp * sr;
x2 = cy * cp * sr - sy * sp * cr;
y2 = sy * cp * sr + cy * sp * cr;
z2 = sy * cp * cr - cy * sp * sr;
q1 = [x2 y2 z2 w2];

%% Rotation matrix to quaternion
R_GL = Rot3D([0, theta, 0]);

Qxx = R_GL(1, 1); Qxy = R_GL(1, 2); Qxz = R_GL(1, 3);
Qyx = R_GL(2, 1); Qyy = R_GL(2, 2); Qyz = R_GL(2, 3);
Qzx = R_GL(3, 1); Qzy = R_GL(3, 2); Qzz = R_GL(3, 3);

t = trace(R_GL);
r = sqrt(1 + t);
w = r / 2;
x = sign(Qzy - Qyz) * sqrt(1 + Qxx - Qyy - Qzz) / 2;
y = sign(Qxz - Qzx) * sqrt(1 - Qxx + Qyy - Qzz) / 2;
z = sign(Qyx - Qxy) * sqrt(1 - Qxx - Qyy + Qzz) / 2;
n = sqrt(x*x + y*y + z*z + w*w);

q2 = [x, y, z, w];