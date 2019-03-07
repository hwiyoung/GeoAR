%사진의 꼭지점에서 지상의 투영되는 꼭지점 좌표 계산
% 2013. 04. 04 
% 전의익
% 01.12.2016 modified by Hwiyoung Kim

function proj_coord = xy_g_min(EO, R, xy_image, nv, d)
% Purpose: Computing ground coordinates corresponding to image vertices
% Output - 01.12.2016
%   proj_coord: ground coordinates corresponding to image vertex in Ground Coordinate System (3x1)
% Input
%   EO: position/attitude parameter of a camera (1x6)
%   R: rotation matrix (3x3)
%   xy_image: image vertex coordinate in Camera Coordinate System (3x1)
%   nv: normal vector of target surface (3x1)
%   d: constant of target surface (1x1)

    scale = (d - nv' * EO(1:3)') / (nv' * R' * xy_image);
    proj_coord = scale * R' * xy_image + EO(1:3)';
end


